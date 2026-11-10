package com.creditsentinel.web;

public record FacilityRow(
        String borrowerName,
        String sector,
        String rating,
        String facilityType,
        String committedLimit,
        String outstanding,
        String utilization,
        boolean overLimit,
        String covenantSummary,
        boolean covenantBreach) {
}
