package com.creditsentinel.agent;

import com.creditsentinel.domain.BreachType;
import com.creditsentinel.domain.CovenantType;
import com.creditsentinel.domain.Recommendation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Stand-in chat model for tests and the offline demo. It still speaks the tool-call protocol.
 * The agent loop, the Java tools, and the citation check are the same objects used in live mode.
 */
public class ScriptedCreditChatModel implements ChatModel {

    private static final Pattern FIELD = Pattern.compile("(?m)^(\\w+(?: \\w+)*):\\s*(\\S+)\\s*$");

    private final ObjectMapper objectMapper;

    public ScriptedCreditChatModel(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        String userText = prompt.getInstructions().stream()
                .filter(message -> !(message instanceof ToolResponseMessage))
                .map(Message::getText)
                .reduce("", (left, right) -> left + "\n" + right);
        boolean toolsAlreadyRan = prompt.getInstructions().stream().anyMatch(ToolResponseMessage.class::isInstance);
        try {
            if (!toolsAlreadyRan) {
                return toolCallTurn(userText);
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(memoJson(prompt, userText)))));
        } catch (Exception exception) {
            return new ChatResponse(List.of(new Generation(
                    new AssistantMessage("{\"error\":\"" + exception.getMessage() + "\"}"))));
        }
    }

    private ChatResponse toolCallTurn(String userText) throws Exception {
        String borrowerId = field(userText, "Borrower id");
        String facilityId = field(userText, "Facility id");
        String sector = field(userText, "Sector");
        List<AssistantMessage.ToolCall> calls = List.of(
                call("call_profile", "getBorrowerProfile", Map.of("borrowerId", borrowerId)),
                call("call_exposure", "getFacilityExposure", Map.of("facilityId", facilityId)),
                call("call_covenant", "getCovenantStatus", Map.of("facilityId", facilityId)),
                call("call_rating", "getRatingHistory", Map.of("borrowerId", borrowerId)),
                call("call_sector", "getSectorConcentration", Map.of("sector", sector)),
                call("call_prior", "searchPriorCases", Map.of("borrowerId", borrowerId)));
        AssistantMessage assistant = AssistantMessage.builder().content("").toolCalls(calls).build();
        return new ChatResponse(List.of(new Generation(assistant)));
    }

    private AssistantMessage.ToolCall call(String id, String name, Map<String, String> arguments) throws Exception {
        return new AssistantMessage.ToolCall(id, "function", name, objectMapper.writeValueAsString(arguments));
    }

    private String memoJson(Prompt prompt, String userText) throws Exception {
        Map<String, JsonNode> tools = new LinkedHashMap<>();
        for (Message message : prompt.getInstructions()) {
            if (message instanceof ToolResponseMessage toolMessage) {
                for (ToolResponseMessage.ToolResponse response : toolMessage.getResponses()) {
                    tools.put(response.name(), objectMapper.readTree(response.responseData()));
                }
            }
        }
        JsonNode profile = required(tools, "getBorrowerProfile");
        JsonNode exposure = required(tools, "getFacilityExposure");
        JsonNode covenants = required(tools, "getCovenantStatus");
        JsonNode rating = required(tools, "getRatingHistory");
        JsonNode concentration = required(tools, "getSectorConcentration");
        JsonNode prior = required(tools, "searchPriorCases");
        BreachType breachType = BreachType.valueOf(field(userText, "Breach type"));
        JsonNode dscr = covenantOfType(covenants, "MIN_DSCR");
        Recommendation recommendation = recommend(breachType, covenants, dscr);

        ArrayNode facts = objectMapper.createArrayNode();
        cite(facts, "outstanding", exposure, "outstanding", "getFacilityExposure");
        cite(facts, "committedLimit", exposure, "committedLimit", "getFacilityExposure");
        cite(facts, "utilization", exposure, "utilization", "getFacilityExposure");
        cite(facts, "latestRating", rating, "latest", "getRatingHistory");
        cite(facts, "sectorShare", concentration, "share", "getSectorConcentration");
        cite(facts, "policyCap", concentration, "policyCap", "getSectorConcentration");
        cite(facts, "priorCaseCount", prior, "priorCaseCount", "searchPriorCases");
        if (dscr != null) {
            cite(facts, "dscrActual", dscr, "actual", "getCovenantStatus");
            cite(facts, "dscrThreshold", dscr, "threshold", "getCovenantStatus");
        }

        ArrayNode drivers = objectMapper.createArrayNode();
        if (exposure.path("overLimit").asBoolean()) {
            drivers.add("Outstanding is above the committed limit.");
        }
        covenants.path("covenants").forEach(line -> {
            if (line.path("breached").asBoolean()) {
                drivers.add(line.path("type").asText() + " is in breach.");
            }
        });
        if (concentration.path("abovePolicyCap").asBoolean()) {
            drivers.add("Sector share is above the portfolio cap.");
        }
        if (rating.path("crossedBelowInvestmentGrade").asBoolean()) {
            drivers.add("Rating crossed out of investment grade.");
        }
        if (drivers.isEmpty()) {
            drivers.add("See the cited facts.");
        }

        String summary = profile.path("legalName").asText()
                + " has outstanding " + exposure.path("outstanding").asText()
                + " against a committed limit of " + exposure.path("committedLimit").asText()
                + " (utilization " + exposure.path("utilization").asText()
                + "). Latest internal rating is " + rating.path("latest").asText()
                + ". " + concentration.path("sector").asText()
                + " is " + concentration.path("share").asText()
                + " of book outstanding versus a cap of " + concentration.path("policyCap").asText()
                + ". Prior approved or rejected cases on file: " + prior.path("priorCaseCount").asText()
                + ". This draft recommends " + recommendation
                + ". It does not change the limit, the rating, or any covenant.";

        ObjectNode memo = objectMapper.createObjectNode();
        memo.put("summary", summary);
        memo.set("citedFacts", facts);
        memo.set("riskDrivers", drivers);
        memo.put("recommendation", recommendation.name());
        memo.putArray("missingData").add("No borrower explanation is on file. The tools only see the book of record.");
        memo.put("confidence", "MEDIUM");
        return objectMapper.writeValueAsString(memo);
    }

    private Recommendation recommend(BreachType breachType, JsonNode covenants, JsonNode dscr) {
        return switch (breachType) {
            case LIMIT_UTILIZATION -> DraftingGuidance.forLimitBreach();
            case RATING_WATCH -> DraftingGuidance.forRatingWatch();
            case COVENANT -> {
                JsonNode breached = firstBreached(covenants);
                if (breached == null) {
                    yield Recommendation.ESCALATE;
                }
                yield DraftingGuidance.forCovenant(
                        CovenantType.valueOf(breached.path("type").asText()),
                        new BigDecimal(breached.path("actual").asText()),
                        new BigDecimal(breached.path("threshold").asText()));
            }
        };
    }

    private static JsonNode required(Map<String, JsonNode> tools, String name) {
        JsonNode node = tools.get(name);
        if (node == null) {
            throw new IllegalStateException("missing " + name);
        }
        return node;
    }

    private static JsonNode covenantOfType(JsonNode covenants, String type) {
        for (JsonNode line : covenants.path("covenants")) {
            if (type.equals(line.path("type").asText())) {
                return line;
            }
        }
        return null;
    }

    private static JsonNode firstBreached(JsonNode covenants) {
        for (JsonNode line : covenants.path("covenants")) {
            if (line.path("breached").asBoolean()) {
                return line;
            }
        }
        return null;
    }

    private void cite(ArrayNode facts, String name, JsonNode source, String field, String tool) {
        ObjectNode fact = facts.addObject();
        fact.put("name", name);
        fact.put("value", source.path(field).asText());
        fact.put("sourceTool", tool);
    }

    static String field(String text, String label) {
        Matcher matcher = FIELD.matcher(text);
        while (matcher.find()) {
            if (matcher.group(1).equals(label)) {
                return matcher.group(2);
            }
        }
        throw new IllegalStateException("Prompt is missing " + label);
    }
}
