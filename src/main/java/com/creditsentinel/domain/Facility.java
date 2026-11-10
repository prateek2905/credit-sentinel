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
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "facility")
public class Facility {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacilityType facilityType;

    @Column(nullable = false)
    private BigDecimal committedLimit;

    @Column(nullable = false)
    private BigDecimal outstanding;

    @Column(nullable = false, length = 3)
    private String currency;

    protected Facility() {
    }

    public UUID getId() {
        return id;
    }

    public Borrower getBorrower() {
        return borrower;
    }

    public FacilityType getFacilityType() {
        return facilityType;
    }

    public BigDecimal getCommittedLimit() {
        return committedLimit;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }

    public String getCurrency() {
        return currency;
    }
}
