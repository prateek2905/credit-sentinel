package com.creditsentinel.service;

import com.creditsentinel.agent.CreditReviewAgent;
import com.creditsentinel.domain.AlertStatus;
import com.creditsentinel.domain.CaseStatus;
import com.creditsentinel.domain.CreditAlert;
import com.creditsentinel.domain.CreditCase;
import com.creditsentinel.repo.AgentStepRepository;
import com.creditsentinel.repo.CreditAlertRepository;
import com.creditsentinel.repo.CreditCaseRepository;
import com.creditsentinel.repo.CreditMemoRepository;
import jakarta.persistence.EntityManager;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvestigationService {

    private final CreditAlertRepository alerts;
    private final CreditCaseRepository cases;
    private final CreditMemoRepository memos;
    private final AgentStepRepository steps;
    private final CreditReviewAgent agent;
    private final EntityManager entityManager;

    public InvestigationService(
            CreditAlertRepository alerts,
            CreditCaseRepository cases,
            CreditMemoRepository memos,
            AgentStepRepository steps,
            CreditReviewAgent agent,
            EntityManager entityManager) {
        this.alerts = alerts;
        this.cases = cases;
        this.memos = memos;
        this.steps = steps;
        this.agent = agent;
        this.entityManager = entityManager;
    }

    @Transactional
    public UUID investigate(UUID alertId) {
        CreditAlert alert = alerts.findById(alertId)
                .orElseThrow(() -> new NotFoundException("No alert " + alertId));
        CreditCase existing = cases.findByAlertId(alertId).orElse(null);
        if (existing != null && EnumSet.of(CaseStatus.PENDING_APPROVAL, CaseStatus.APPROVED, CaseStatus.REJECTED)
                .contains(existing.getStatus())) {
            return existing.getId();
        }
        CreditCase creditCase = existing == null ? cases.save(CreditCase.forAlert(alert)) : existing;
        memos.findByCreditCaseId(creditCase.getId()).ifPresent(memos::delete);
        steps.deleteByCaseId(creditCase.getId());
        entityManager.flush();
        creditCase.setFailureReason(null);
        creditCase.setStatus(CaseStatus.INVESTIGATING);
        alert.setStatus(AlertStatus.INVESTIGATING);
        agent.investigate(creditCase);
        return creditCase.getId();
    }
}
