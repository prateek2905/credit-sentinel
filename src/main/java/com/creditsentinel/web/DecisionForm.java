package com.creditsentinel.web;

import com.creditsentinel.domain.DecisionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DecisionForm {

    @NotBlank
    @Size(max = 80)
    private String analystId = "analyst.demo";

    @NotNull
    private DecisionType decision;

    @NotBlank
    @Size(max = 2000)
    private String comment;

    public String getAnalystId() {
        return analystId;
    }

    public void setAnalystId(String analystId) {
        this.analystId = analystId;
    }

    public DecisionType getDecision() {
        return decision;
    }

    public void setDecision(DecisionType decision) {
        this.decision = decision;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
