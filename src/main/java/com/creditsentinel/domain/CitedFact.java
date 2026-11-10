package com.creditsentinel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "cited_fact")
public class CitedFact {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "memo_id", nullable = false)
    private CreditMemo memo;

    @Column(name = "fact_name", nullable = false)
    private String factName;

    @Column(name = "fact_value", nullable = false)
    private String factValue;

    @Column(name = "source_tool", nullable = false)
    private String sourceTool;

    @Column(nullable = false)
    private int position;

    protected CitedFact() {
    }

    CitedFact(CreditMemo memo, String factName, String factValue, String sourceTool, int position) {
        this.id = UUID.randomUUID();
        this.memo = memo;
        this.factName = factName;
        this.factValue = factValue;
        this.sourceTool = sourceTool;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public String getFactName() {
        return factName;
    }

    public String getFactValue() {
        return factValue;
    }

    public String getSourceTool() {
        return sourceTool;
    }
}
