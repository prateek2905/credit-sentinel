package com.creditsentinel.detection;

import com.creditsentinel.domain.BreachType;
import com.creditsentinel.domain.math.CovenantMath;
import com.creditsentinel.domain.math.ExposureMath;
import com.creditsentinel.domain.math.RatingWatch;
import java.util.ArrayList;
import java.util.List;

public final class BreachDetector {

    public List<Breach> detect(FacilitySnapshot snapshot) {
        List<Breach> breaches = new ArrayList<>();
        if (ExposureMath.overLimit(snapshot.outstanding(), snapshot.committedLimit())) {
            breaches.add(new Breach(
                    snapshot.facilityId(),
                    BreachType.LIMIT_UTILIZATION,
                    "Outstanding " + ExposureMath.money(snapshot.outstanding())
                            + " exceeds committed limit " + ExposureMath.money(snapshot.committedLimit())));
        }
        List<String> covenantFailures = new ArrayList<>();
        for (CovenantSnapshot covenant : snapshot.covenants()) {
            if (CovenantMath.breached(covenant.type(), covenant.actual(), covenant.threshold())) {
                covenantFailures.add(covenant.type()
                        + " actual " + ExposureMath.ratio(covenant.actual())
                        + " is on the wrong side of " + ExposureMath.ratio(covenant.threshold()));
            }
        }
        if (!covenantFailures.isEmpty()) {
            breaches.add(new Breach(
                    snapshot.facilityId(),
                    BreachType.COVENANT,
                    String.join("; ", covenantFailures)));
        }
        if (RatingWatch.crossedBelowInvestmentGrade(snapshot.previousRating(), snapshot.latestRating())) {
            breaches.add(new Breach(
                    snapshot.facilityId(),
                    BreachType.RATING_WATCH,
                    "Rating moved from " + snapshot.previousRating()
                            + " to " + snapshot.latestRating()
                            + ", crossing out of investment grade"));
        }
        return List.copyOf(breaches);
    }
}
