package com.creditsentinel.agent;

import com.creditsentinel.domain.CovenantType;
import com.creditsentinel.domain.Recommendation;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Drafting default used by the scripted narrator and written into the live prompt.
 * It does not change a limit, grant a waiver, or set a rating. The human decision is separate.
 */
public final class DraftingGuidance {

    static final BigDecimal MATERIAL_DSCR_GAP = new BigDecimal("0.10");

    private DraftingGuidance() {
    }

    public static Recommendation forLimitBreach() {
        return Recommendation.REDUCE_LIMIT;
    }

    public static Recommendation forRatingWatch() {
        return Recommendation.ESCALATE;
    }

    public static Recommendation forCovenant(CovenantType type, BigDecimal actual, BigDecimal threshold) {
        if (type != CovenantType.MIN_DSCR) {
            return Recommendation.ESCALATE;
        }
        BigDecimal gap = threshold.subtract(actual).divide(threshold, 4, RoundingMode.HALF_UP);
        if (gap.compareTo(MATERIAL_DSCR_GAP) > 0) {
            return Recommendation.ESCALATE;
        }
        return Recommendation.WAIVE_WITH_CONDITIONS;
    }
}
