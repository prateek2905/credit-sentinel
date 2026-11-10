package com.creditsentinel.book;

public record PriorCaseView(
        String caseId,
        String trigger,
        String status,
        String recommendation,
        String summary) {
}
