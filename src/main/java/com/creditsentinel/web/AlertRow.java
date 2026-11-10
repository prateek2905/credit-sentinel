package com.creditsentinel.web;

import java.util.UUID;

public record AlertRow(
        UUID alertId,
        String borrowerName,
        String breachType,
        String detail,
        String status,
        UUID caseId,
        String caseStatus) {
}
