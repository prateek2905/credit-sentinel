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
@Table(name = "credit_alert")
public class CreditAlert {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BreachType breachType;

    @Column(nullable = false, columnDefinition = "text")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected CreditAlert() {
    }

    public static CreditAlert open(Facility facility, BreachType breachType, String detail) {
        CreditAlert alert = new CreditAlert();
        alert.id = UUID.randomUUID();
        alert.facility = facility;
        alert.breachType = breachType;
        alert.detail = detail;
        alert.status = AlertStatus.OPEN;
        alert.createdAt = Instant.now();
        return alert;
    }

    public UUID getId() {
        return id;
    }

    public Facility getFacility() {
        return facility;
    }

    public BreachType getBreachType() {
        return breachType;
    }

    public String getDetail() {
        return detail;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
