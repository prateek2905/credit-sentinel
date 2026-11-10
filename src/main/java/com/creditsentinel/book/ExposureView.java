package com.creditsentinel.book;

public record ExposureView(
        String borrowerId,
        String facilityId,
        String facilityType,
        String committedLimit,
        String outstanding,
        String utilization,
        boolean overLimit,
        String currency) {
}
