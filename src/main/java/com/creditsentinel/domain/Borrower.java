package com.creditsentinel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "borrower")
public class Borrower {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String legalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sector sector;

    @Column(nullable = false)
    private String headquarters;

    @Column(nullable = false)
    private LocalDate relationshipStart;

    protected Borrower() {
    }

    public UUID getId() {
        return id;
    }

    public String getLegalName() {
        return legalName;
    }

    public Sector getSector() {
        return sector;
    }

    public String getHeadquarters() {
        return headquarters;
    }

    public LocalDate getRelationshipStart() {
        return relationshipStart;
    }
}
