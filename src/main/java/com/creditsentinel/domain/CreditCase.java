package com.creditsentinel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credit_case")
public class CreditCase {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id")
    private CreditAlert alert;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Enumerated(EnumType.STRING)
    private BreachType breachType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseTrigger caseTrigger;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    @Column(columnDefinition = "text")
    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    protected CreditCase() {
    }

    public static CreditCase forAlert(CreditAlert alert) {
        CreditCase creditCase = new CreditCase();
        creditCase.id = UUID.randomUUID();
        creditCase.alert = alert;
        creditCase.borrower = alert.getFacility().getBorrower();
        creditCase.facility = alert.getFacility();
        creditCase.breachType = alert.getBreachType();
        creditCase.caseTrigger = CaseTrigger.BREACH;
        creditCase.status = CaseStatus.INVESTIGATING;
        creditCase.createdAt = Instant.now();
        return creditCase;
    }

    public UUID getId() {
        return id;
    }

    public CreditAlert getAlert() {
        return alert;
    }

    public Borrower getBorrower() {
        return borrower;
    }

    public Facility getFacility() {
        return facility;
    }

    public BreachType getBreachType() {
        return breachType;
    }

    public CaseTrigger getCaseTrigger() {
        return caseTrigger;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
