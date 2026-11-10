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
import java.util.UUID;

@Entity
@Table(name = "covenant")
public class Covenant {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CovenantType covenantType;

    protected Covenant() {
    }

    public UUID getId() {
        return id;
    }

    public Facility getFacility() {
        return facility;
    }

    public CovenantType getCovenantType() {
        return covenantType;
    }
}
