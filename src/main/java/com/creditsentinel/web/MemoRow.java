package com.creditsentinel.web;

import java.util.List;

public record MemoRow(
        String summary,
        String recommendation,
        String confidence,
        List<String> riskDrivers,
        List<String> missingData,
        List<FactRow> citedFacts) {
}
