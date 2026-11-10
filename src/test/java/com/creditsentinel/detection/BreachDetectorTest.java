package com.creditsentinel.detection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.creditsentinel.domain.BreachType;
import com.creditsentinel.domain.CovenantType;
import com.creditsentinel.domain.Rating;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BreachDetectorTest {

    private final BreachDetector detector = new BreachDetector();
    private final UUID facilityId = UUID.randomUUID();

    @Test
    void flagsOutstandingAboveTheCommittedLimit() {
        List<Breach> breaches = detector.detect(snapshot(
                new BigDecimal("25000000.00"),
                new BigDecimal("27000000.00"),
                List.of(covenant(CovenantType.MIN_DSCR, "1.2500", "1.8000")),
                Rating.BBB,
                Rating.BBB));

        assertEquals(List.of(BreachType.LIMIT_UTILIZATION), types(breaches));
    }

    @Test
    void doesNotFlagADrawThatEqualsTheLimit() {
        List<Breach> breaches = detector.detect(snapshot(
                new BigDecimal("25000000.00"),
                new BigDecimal("25000000.00"),
                List.of(),
                Rating.BBB,
                Rating.BBB));

        assertTrue(breaches.isEmpty());
    }

    @Test
    void flagsDscrBelowTheMinimumAndIgnoresAPassingLeverageCovenant() {
        List<Breach> breaches = detector.detect(snapshot(
                new BigDecimal("15000000.00"),
                new BigDecimal("9000000.00"),
                List.of(
                        covenant(CovenantType.MIN_DSCR, "1.2500", "1.0500"),
                        covenant(CovenantType.MAX_LEVERAGE, "3.5000", "3.5000")),
                Rating.BB,
                Rating.BB));

        assertEquals(List.of(BreachType.COVENANT), types(breaches));
        assertTrue(breaches.get(0).detail().contains("MIN_DSCR"));
    }

    @Test
    void flagsLeverageAboveTheMaximum() {
        List<Breach> breaches = detector.detect(snapshot(
                new BigDecimal("10000000.00"),
                new BigDecimal("1000000.00"),
                List.of(covenant(CovenantType.MAX_LEVERAGE, "3.5000", "3.5001")),
                null,
                null));

        assertEquals(List.of(BreachType.COVENANT), types(breaches));
    }

    @Test
    void flagsOnlyADowngradeThatLeavesInvestmentGrade() {
        assertTrue(detector.detect(snapshot(limit(), limit(), List.of(), Rating.A, Rating.BBB)).isEmpty());
        assertTrue(detector.detect(snapshot(limit(), limit(), List.of(), Rating.BB, Rating.B)).isEmpty());
        assertEquals(
                List.of(BreachType.RATING_WATCH),
                types(detector.detect(snapshot(limit(), limit(), List.of(), Rating.BBB, Rating.BB))));
    }

    private static List<BreachType> types(List<Breach> breaches) {
        return breaches.stream().map(Breach::type).toList();
    }

    private FacilitySnapshot snapshot(
            BigDecimal limit,
            BigDecimal outstanding,
            List<CovenantSnapshot> covenants,
            Rating previous,
            Rating latest) {
        return new FacilitySnapshot(facilityId, limit, outstanding, covenants, previous, latest);
    }

    private static BigDecimal limit() {
        return new BigDecimal("1000000.00");
    }

    private static CovenantSnapshot covenant(CovenantType type, String threshold, String actual) {
        return new CovenantSnapshot(type, new BigDecimal(threshold), new BigDecimal(actual));
    }
}
