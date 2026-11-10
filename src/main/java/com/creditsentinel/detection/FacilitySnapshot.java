package com.creditsentinel.detection;

import com.creditsentinel.domain.Rating;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FacilitySnapshot(
        UUID facilityId,
        BigDecimal committedLimit,
        BigDecimal outstanding,
        List<CovenantSnapshot> covenants,
        Rating previousRating,
        Rating latestRating) {
}
