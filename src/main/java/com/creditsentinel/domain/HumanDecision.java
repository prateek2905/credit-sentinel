package com.creditsentinel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "human_decision")
public class HumanDecision {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, unique = true)
    private CreditCase creditCase;

    @Column(nullable = false)
    private String analystId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionType decision;

    @Column(nullable = false, columnDefinition = "text")
    private String comment;

    @Column(nullable = false)
    private Instant decidedAt;

    protected HumanDecision() {
    }

    public HumanDecision(CreditCase creditCase, String analystId, DecisionType decision, String comment) {
        this.id = UUID.randomUUID();
        this.creditCase = creditCase;
        this.analystId = analystId;
        this.decision = decision;
        this.comment = comment;
        this.decidedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public CreditCase getCreditCase() {
        return creditCase;
    }

    public String getAnalystId() {
        return analystId;
    }

    public DecisionType getDecision() {
        return decision;
    }

    public String getComment() {
        return comment;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
