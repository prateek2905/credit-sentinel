package com.creditsentinel.web;

import java.util.List;
import java.util.UUID;

public record CasePage(
        UUID id,
        String borrowerName,
        String sector,
        String facilityType,
        String committedLimit,
        String outstanding,
        String status,
        String breachType,
        String failureReason,
        String alertDetail,
        List<StepRow> steps,
        MemoRow memo,
        DecisionRow decision) {
}
