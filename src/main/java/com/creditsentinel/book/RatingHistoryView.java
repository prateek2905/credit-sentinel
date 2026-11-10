package com.creditsentinel.book;

import java.util.List;

public record RatingHistoryView(
        String borrowerId,
        List<RatingEventView> events,
        String latest,
        String previous,
        boolean crossedBelowInvestmentGrade) {
}
