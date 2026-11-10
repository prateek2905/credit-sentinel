package com.creditsentinel.web;

import java.util.UUID;

public record DecisionRow(
        UUID caseId,
        String borrowerName,
        String analystId,
        String decision,
        String comment,
        String decidedAt) {
}
