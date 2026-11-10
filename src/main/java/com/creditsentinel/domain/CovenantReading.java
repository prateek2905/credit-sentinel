package com.creditsentinel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "covenant_reading")
public class CovenantReading {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "covenant_id", nullable = false)
    private Covenant covenant;

    @Column(nullable = false)
    private BigDecimal threshold;

    @Column(nullable = false)
    private BigDecimal actual;

    @Column(nullable = false)
    private LocalDate asOfDate;

    protected CovenantReading() {
    }

    public UUID getId() {
        return id;
    }

    public Covenant getCovenant() {
        return covenant;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public BigDecimal getActual() {
        return actual;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }
}
