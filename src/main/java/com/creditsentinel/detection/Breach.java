package com.creditsentinel.detection;

import com.creditsentinel.domain.BreachType;
import java.util.UUID;

public record Breach(UUID facilityId, BreachType type, String detail) {
}
