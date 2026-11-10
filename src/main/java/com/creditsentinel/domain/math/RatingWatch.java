package com.creditsentinel.domain.math;

import com.creditsentinel.domain.Rating;

public final class RatingWatch {

    private RatingWatch() {
    }

    /**
     * True only when a downgrade crosses the line from investment grade (BBB or stronger)
     * into non-investment grade (BB or weaker). A move that stays on one side of that line
     * is not a watch alert. An upgrade is not a watch alert.
     */
    public static boolean crossedBelowInvestmentGrade(Rating previous, Rating latest) {
        if (previous == null || latest == null) {
            return false;
        }
        return latest.isWorseThan(previous)
                && previous.isInvestmentGrade()
                && !latest.isInvestmentGrade();
    }
}
