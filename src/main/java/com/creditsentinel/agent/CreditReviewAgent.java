package com.creditsentinel.agent;

import com.creditsentinel.config.CreditProperties;
import com.creditsentinel.domain.AgentStep;
import com.creditsentinel.domain.CreditCase;
import com.creditsentinel.domain.CreditMemo;
import com.creditsentinel.domain.StepKind;
import com.creditsentinel.repo.AgentStepRepository;
import com.creditsentinel.repo.CreditMemoRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class CreditReviewAgent {

    private static final Logger log = LoggerFactory.getLogger(CreditReviewAgent.class);

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final List<ToolCallback> toolCallbacks;
    private final MemoParser parser;
    private final CitationVerifier verifier;
    private final AgentStepRepository steps;
    private final CreditMemoRepository memos;
    private final MeterRegistry meterRegistry;
    private final CreditProperties properties;

    public CreditReviewAgent(
            ChatModel chatModel,
            ToolCallingManager toolCallingManager,
            List<ToolCallback> toolCallbacks,
            MemoParser parser,
            CitationVerifier verifier,
            AgentStepRepository steps,
            CreditMemoRepository memos,
            MeterRegistry meterRegistry,
            CreditProperties properties) {
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
        this.toolCallbacks = toolCallbacks;
        this.parser = parser;
        this.verifier = verifier;
        this.steps = steps;
        this.memos = memos;
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    public void investigate(CreditCase creditCase) {
        MDC.put("caseId", creditCase.getId().toString());
        try {
            run(creditCase);
        } catch (RuntimeException exception) {
            log.warn("investigation failed caseId={}", creditCase.getId(), exception);
            fail(creditCase, exception.getMessage() == null ? exception.toString() : exception.getMessage());
        } finally {
            MDC.remove("caseId");
        }
    }

    private void run(CreditCase creditCase) {
        List<ToolEvidence> evidence = new ArrayList<>();
        AtomicInteger stepIndex = new AtomicInteger();
        List<ToolCallback> auditing = toolCallbacks.stream()
                .map(callback -> (ToolCallback) new AuditingToolCallback(callback, creditCase, evidence, stepIndex))
                .toList();
        var options = DefaultToolCallingChatOptions.builder()
                .toolCallbacks(auditing)
                .internalToolExecutionEnabled(false)
                .temperature(0.0)
                .build();
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(InvestigationPrompt.system()));
        messages.add(new UserMessage(InvestigationPrompt.user(
                creditCase.getBreachType(),
                creditCase.getBorrower().getId(),
                creditCase.getFacility().getId(),
                creditCase.getBorrower().getSector())));

        int repairs = 0;
        int turns = 0;
        while (turns < properties.getMaxModelTurns()) {
            turns++;
            Prompt prompt = new Prompt(messages, options);
            long started = System.nanoTime();
            ChatResponse response = chatModel.call(prompt);
            record("agent.step", creditCase, "kind", "MODEL_TURN", started);
            AssistantMessage assistant = response.getResult().getOutput();
            messages.add(assistant);
            recordStep(creditCase, stepIndex, StepKind.MODEL_TURN, null, modelPayload(assistant));

            if (response.hasToolCalls()) {
                ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response);
                messages = new ArrayList<>(result.conversationHistory());
                continue;
            }

            ParseResult parsed = parser.parse(assistant.getText());
            if (!parsed.ok()) {
                recordStep(creditCase, stepIndex, StepKind.VERIFIER, null, parsed.error());
                if (repairs >= 1) {
                    fail(creditCase, parsed.error());
                    return;
                }
                repairs++;
                messages.add(new UserMessage(InvestigationPrompt.repair(parsed.error())));
                continue;
            }

            VerificationResult verification = verifier.verify(parsed.draft(), evidence);
            recordStep(
                    creditCase,
                    stepIndex,
                    StepKind.VERIFIER,
                    null,
                    verification.passed() ? "passed" : String.join("; ", verification.errors()));
            if (verification.passed()) {
                saveMemo(creditCase, parsed.draft());
                creditCase.setStatus(com.creditsentinel.domain.CaseStatus.PENDING_APPROVAL);
                creditCase.setFailureReason(null);
                log.info("memo drafted caseId={} recommendation={}", creditCase.getId(), parsed.draft().recommendation());
                return;
            }
            if (repairs >= 1) {
                fail(creditCase, String.join("; ", verification.errors()));
                return;
            }
            repairs++;
            messages.add(new UserMessage(InvestigationPrompt.repair(String.join("; ", verification.errors()))));
        }
        fail(creditCase, "Stopped after " + properties.getMaxModelTurns() + " model turns");
    }

    private void saveMemo(CreditCase creditCase, MemoDraft draft) {
        memos.findByCreditCaseId(creditCase.getId()).ifPresent(memos::delete);
        CreditMemo memo = new CreditMemo(
                creditCase, draft.summary(), draft.recommendation(), draft.confidence());
        for (CitedFactDraft fact : draft.citedFacts()) {
            memo.addFact(fact.name(), fact.value(), fact.sourceTool());
        }
        memo.getRiskDrivers().addAll(draft.riskDrivers());
        memo.getMissingData().addAll(draft.missingData());
        memos.save(memo);
    }

    private void fail(CreditCase creditCase, String reason) {
        creditCase.setStatus(com.creditsentinel.domain.CaseStatus.FAILED);
        creditCase.setFailureReason(reason);
        if (creditCase.getAlert() != null) {
            creditCase.getAlert().setStatus(com.creditsentinel.domain.AlertStatus.OPEN);
        }
        log.info("investigation failed caseId={} reason={}", creditCase.getId(), reason);
    }

    private void recordStep(
            CreditCase creditCase, AtomicInteger stepIndex, StepKind kind, String toolName, String payload) {
        steps.save(AgentStep.of(creditCase.getId(), stepIndex.getAndIncrement(), kind, toolName, payload));
    }

    private void record(String metric, CreditCase creditCase, String tagKey, String tagValue, long startedNanos) {
        Timer.builder(metric)
                .tag("caseId", creditCase.getId().toString())
                .tag(tagKey, tagValue)
                .register(meterRegistry)
                .record(Duration.ofNanos(System.nanoTime() - startedNanos));
    }

    private static String modelPayload(AssistantMessage assistant) {
        if (assistant.hasToolCalls()) {
            return assistant.getToolCalls().stream().map(AssistantMessage.ToolCall::name).toList().toString();
        }
        return assistant.getText() == null ? "" : assistant.getText();
    }

    private final class AuditingToolCallback implements ToolCallback {

        private final ToolCallback delegate;
        private final CreditCase creditCase;
        private final List<ToolEvidence> evidence;
        private final AtomicInteger stepIndex;

        private AuditingToolCallback(
                ToolCallback delegate,
                CreditCase creditCase,
                List<ToolEvidence> evidence,
                AtomicInteger stepIndex) {
            this.delegate = delegate;
            this.creditCase = creditCase;
            this.evidence = evidence;
            this.stepIndex = stepIndex;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, @Nullable ToolContext toolContext) {
            String name = delegate.getToolDefinition().name();
            recordStep(creditCase, stepIndex, StepKind.TOOL_CALL, name, toolInput);
            long started = System.nanoTime();
            String output = delegate.call(toolInput, toolContext);
            evidence.add(new ToolEvidence(name, toolInput, output));
            recordStep(creditCase, stepIndex, StepKind.TOOL_RESULT, name, output);
            Timer.builder("tool.call")
                    .tag("caseId", creditCase.getId().toString())
                    .tag("tool", name)
                    .register(meterRegistry)
                    .record(Duration.ofNanos(System.nanoTime() - started));
            return output;
        }
    }
}
