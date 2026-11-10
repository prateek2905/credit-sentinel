package com.creditsentinel.web;

import com.creditsentinel.book.BookReader;
import com.creditsentinel.book.CovenantLineView;
import com.creditsentinel.book.CovenantStatusView;
import com.creditsentinel.domain.AlertStatus;
import com.creditsentinel.domain.CreditAlert;
import com.creditsentinel.domain.CreditCase;
import com.creditsentinel.domain.Facility;
import com.creditsentinel.domain.HumanDecision;
import com.creditsentinel.domain.math.ExposureMath;
import com.creditsentinel.repo.AgentStepRepository;
import com.creditsentinel.repo.CreditAlertRepository;
import com.creditsentinel.repo.CreditCaseRepository;
import com.creditsentinel.repo.CreditMemoRepository;
import com.creditsentinel.repo.HumanDecisionRepository;
import com.creditsentinel.service.NotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DeskQuery {

    private final BookReader book;
    private final CreditAlertRepository alerts;
    private final CreditCaseRepository cases;
    private final CreditMemoRepository memos;
    private final AgentStepRepository steps;
    private final HumanDecisionRepository decisions;

    public DeskQuery(
            BookReader book,
            CreditAlertRepository alerts,
            CreditCaseRepository cases,
            CreditMemoRepository memos,
            AgentStepRepository steps,
            HumanDecisionRepository decisions) {
        this.book = book;
        this.alerts = alerts;
        this.cases = cases;
        this.memos = memos;
        this.steps = steps;
        this.decisions = decisions;
    }

    public DeskPage desk() {
        List<AlertRow> openAlerts = alerts.findByStatusNotOrderByCreatedAtAsc(AlertStatus.CLOSED).stream()
                .map(this::alertRow)
                .toList();
        List<FacilityRow> facilities = new ArrayList<>();
        for (Facility facility : book.allFacilities()) {
            facilities.add(facilityRow(facility));
        }
        facilities.sort(Comparator.comparing(FacilityRow::borrowerName));
        List<DecisionRow> recent = decisions.findAllByOrderByDecidedAtDesc().stream()
                .map(this::decisionRow)
                .toList();
        return new DeskPage(openAlerts, List.copyOf(facilities), recent);
    }

    public CasePage casePage(UUID caseId) {
        CreditCase creditCase = cases.findById(caseId)
                .orElseThrow(() -> new NotFoundException("No case " + caseId));
        var memo = memos.findByCreditCaseId(caseId).orElse(null);
        MemoRow memoRow = null;
        if (memo != null) {
            memoRow = new MemoRow(
                    memo.getSummary(),
                    memo.getRecommendation().name(),
                    memo.getConfidence().name(),
                    List.copyOf(memo.getRiskDrivers()),
                    List.copyOf(memo.getMissingData()),
                    memo.getCitedFacts().stream()
                            .map(fact -> new FactRow(fact.getFactName(), fact.getFactValue(), fact.getSourceTool()))
                            .toList());
        }
        HumanDecision decision = decisions.findByCreditCaseId(caseId).orElse(null);
        return new CasePage(
                creditCase.getId(),
                creditCase.getBorrower().getLegalName(),
                creditCase.getBorrower().getSector().name(),
                creditCase.getFacility().getFacilityType().name(),
                ExposureMath.money(creditCase.getFacility().getCommittedLimit()),
                ExposureMath.money(creditCase.getFacility().getOutstanding()),
                creditCase.getStatus().name(),
                creditCase.getBreachType() == null ? "PRIOR_REVIEW" : creditCase.getBreachType().name(),
                creditCase.getFailureReason(),
                creditCase.getAlert() == null ? null : creditCase.getAlert().getDetail(),
                stepsFor(caseId),
                memoRow,
                decision == null ? null : decisionRow(decision));
    }

    public List<StepRow> audit(UUID caseId) {
        if (caseId != null && !cases.existsById(caseId)) {
            throw new NotFoundException("No case " + caseId);
        }
        if (caseId != null) {
            return stepsFor(caseId);
        }
        return steps.findAll().stream()
                .sorted(Comparator.comparing(com.creditsentinel.domain.AgentStep::getCreatedAt))
                .map(DeskQuery::stepRow)
                .toList();
    }

    private List<StepRow> stepsFor(UUID caseId) {
        return steps.findByCaseIdOrderByStepIndexAsc(caseId).stream().map(DeskQuery::stepRow).toList();
    }

    private static StepRow stepRow(com.creditsentinel.domain.AgentStep step) {
        return new StepRow(
                step.getCaseId(),
                step.getStepIndex(),
                step.getKind().name(),
                step.getToolName(),
                step.getPayload(),
                step.getCreatedAt().toString());
    }

    private AlertRow alertRow(CreditAlert alert) {
        CreditCase creditCase = cases.findByAlertId(alert.getId()).orElse(null);
        return new AlertRow(
                alert.getId(),
                alert.getFacility().getBorrower().getLegalName(),
                alert.getBreachType().name(),
                alert.getDetail(),
                alert.getStatus().name(),
                creditCase == null ? null : creditCase.getId(),
                creditCase == null ? null : creditCase.getStatus().name());
    }

    private FacilityRow facilityRow(Facility facility) {
        var exposure = book.exposure(facility.getId());
        CovenantStatusView covenants = book.covenantStatus(facility.getId());
        String covenantSummary = covenants.covenants().stream()
                .map(line -> line.type() + " " + line.actual() + " vs " + line.threshold()
                        + (line.breached() ? " BREACH" : ""))
                .reduce((left, right) -> left + "; " + right)
                .orElse("none");
        boolean covenantBreach = covenants.covenants().stream().anyMatch(CovenantLineView::breached);
        var history = book.ratingHistory(facility.getBorrower().getId());
        BigDecimal utilization = new BigDecimal(exposure.utilization());
        return new FacilityRow(
                facility.getBorrower().getLegalName(),
                facility.getBorrower().getSector().name(),
                history.latest(),
                facility.getFacilityType().name(),
                exposure.committedLimit(),
                exposure.outstanding(),
                utilization.movePointRight(2).setScale(2) + "%",
                exposure.overLimit(),
                covenantSummary,
                covenantBreach);
    }

    private DecisionRow decisionRow(HumanDecision decision) {
        CreditCase creditCase = decision.getCreditCase();
        return new DecisionRow(
                creditCase.getId(),
                creditCase.getBorrower().getLegalName(),
                decision.getAnalystId(),
                decision.getDecision().name(),
                decision.getComment(),
                decision.getDecidedAt().toString());
    }
}
