package com.creditsentinel.detection;

import com.creditsentinel.domain.CovenantType;
import java.math.BigDecimal;

public record CovenantSnapshot(CovenantType type, BigDecimal threshold, BigDecimal actual) {
}
