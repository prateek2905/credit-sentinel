package com.creditsentinel.web;

import java.util.UUID;

public record StepRow(
        UUID caseId,
        int index,
        String kind,
        String toolName,
        String payload,
        String createdAt) {
}
