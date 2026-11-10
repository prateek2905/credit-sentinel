package com.creditsentinel.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "credit_memo")
public class CreditMemo {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, unique = true)
    private CreditCase creditCase;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Recommendation recommendation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Confidence confidence;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "memo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<CitedFact> citedFacts = new ArrayList<>();

    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "memo_risk_driver", joinColumns = @JoinColumn(name = "memo_id"))
    @OrderColumn(name = "position")
    @Column(name = "driver", nullable = false, columnDefinition = "text")
    private List<String> riskDrivers = new ArrayList<>();

    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "memo_missing_data", joinColumns = @JoinColumn(name = "memo_id"))
    @OrderColumn(name = "position")
    @Column(name = "item", nullable = false, columnDefinition = "text")
    private List<String> missingData = new ArrayList<>();

    protected CreditMemo() {
    }

    public CreditMemo(CreditCase creditCase, String summary, Recommendation recommendation, Confidence confidence) {
        this.id = UUID.randomUUID();
        this.creditCase = creditCase;
        this.summary = summary;
        this.recommendation = recommendation;
        this.confidence = confidence;
        this.createdAt = Instant.now();
    }

    public void addFact(String name, String value, String sourceTool) {
        citedFacts.add(new CitedFact(this, name, value, sourceTool, citedFacts.size()));
    }

    public UUID getId() {
        return id;
    }

    public CreditCase getCreditCase() {
        return creditCase;
    }

    public String getSummary() {
        return summary;
    }

    public Recommendation getRecommendation() {
        return recommendation;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<CitedFact> getCitedFacts() {
        return citedFacts;
    }

    public List<String> getRiskDrivers() {
        return riskDrivers;
    }

    public List<String> getMissingData() {
        return missingData;
    }
}
