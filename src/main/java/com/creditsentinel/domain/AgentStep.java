package com.creditsentinel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_step")
public class AgentStep {

    @Id
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(nullable = false)
    private int stepIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_kind", nullable = false)
    private StepKind kind;

    private String toolName;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    protected AgentStep() {
    }

    public static AgentStep of(UUID caseId, int stepIndex, StepKind kind, String toolName, String payload) {
        AgentStep step = new AgentStep();
        step.id = UUID.randomUUID();
        step.caseId = caseId;
        step.stepIndex = stepIndex;
        step.kind = kind;
        step.toolName = toolName;
        step.payload = payload == null ? "" : payload;
        step.createdAt = Instant.now();
        return step;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public StepKind getKind() {
        return kind;
    }

    public String getToolName() {
        return toolName;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
