package com.creditsentinel.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.creditsentinel.domain.CovenantType;
import com.creditsentinel.domain.Recommendation;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DraftingGuidanceTest {

    @Test
    void limitBreachRecommendsAReductionAndDoesNotPickANewNumber() {
        assertEquals(Recommendation.REDUCE_LIMIT, DraftingGuidance.forLimitBreach());
    }

    @Test
    void ratingWatchIsEscalated() {
        assertEquals(Recommendation.ESCALATE, DraftingGuidance.forRatingWatch());
    }

    @Test
    void dscrMissAboveTenPercentOfTheMinimumIsEscalated() {
        assertEquals(
                Recommendation.ESCALATE,
                DraftingGuidance.forCovenant(CovenantType.MIN_DSCR, new BigDecimal("1.0500"), new BigDecimal("1.2500")));
    }

    @Test
    void dscrMissOfExactlyTenPercentIsDraftedAsAConditionalWaiver() {
        assertEquals(
                Recommendation.WAIVE_WITH_CONDITIONS,
                DraftingGuidance.forCovenant(CovenantType.MIN_DSCR, new BigDecimal("1.1250"), new BigDecimal("1.2500")));
    }

    @Test
    void leverageBreachIsEscalated() {
        assertEquals(
                Recommendation.ESCALATE,
                DraftingGuidance.forCovenant(CovenantType.MAX_LEVERAGE, new BigDecimal("4.0000"), new BigDecimal("3.5000")));
    }
}
