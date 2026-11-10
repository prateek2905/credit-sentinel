package com.creditsentinel.domain.math;

import com.creditsentinel.domain.CovenantType;
import java.math.BigDecimal;

public final class CovenantMath {

    private CovenantMath() {
    }

    public static boolean breached(CovenantType type, BigDecimal actual, BigDecimal threshold) {
        return switch (type) {
            case MIN_DSCR -> actual.compareTo(threshold) < 0;
            case MAX_LEVERAGE -> actual.compareTo(threshold) > 0;
        };
    }

    public static String direction(CovenantType type) {
        return switch (type) {
            case MIN_DSCR -> "MINIMUM";
            case MAX_LEVERAGE -> "MAXIMUM";
        };
    }
}
