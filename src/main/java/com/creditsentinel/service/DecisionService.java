package com.creditsentinel.service;

import com.creditsentinel.domain.AlertStatus;
import com.creditsentinel.domain.CaseStatus;
import com.creditsentinel.domain.CreditCase;
import com.creditsentinel.domain.DecisionType;
import com.creditsentinel.domain.HumanDecision;
import com.creditsentinel.repo.CreditCaseRepository;
import com.creditsentinel.repo.HumanDecisionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisionService {

    private final CreditCaseRepository cases;
    private final HumanDecisionRepository decisions;

    public DecisionService(CreditCaseRepository cases, HumanDecisionRepository decisions) {
        this.cases = cases;
        this.decisions = decisions;
    }

    @Transactional
    public void decide(UUID caseId, String analystId, DecisionType decision, String comment) {
        CreditCase creditCase = cases.findById(caseId)
                .orElseThrow(() -> new NotFoundException("No case " + caseId));
        if (creditCase.getStatus() != CaseStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only a case waiting for approval can be decided. Status is " + creditCase.getStatus());
        }
        decisions.save(new HumanDecision(creditCase, analystId.trim(), decision, comment.trim()));
        creditCase.setStatus(decision == DecisionType.APPROVE ? CaseStatus.APPROVED : CaseStatus.REJECTED);
        if (creditCase.getAlert() != null) {
            creditCase.getAlert().setStatus(AlertStatus.CLOSED);
        }
    }
}
