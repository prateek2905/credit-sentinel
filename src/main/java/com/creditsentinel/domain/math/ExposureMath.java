package com.creditsentinel.domain.math;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ExposureMath {

    private ExposureMath() {
    }

    public static BigDecimal utilization(BigDecimal outstanding, BigDecimal committedLimit) {
        if (committedLimit.signum() == 0) {
            throw new IllegalArgumentException("committed limit is zero");
        }
        return outstanding.divide(committedLimit, 4, RoundingMode.HALF_UP);
    }

    public static boolean overLimit(BigDecimal outstanding, BigDecimal committedLimit) {
        return outstanding.compareTo(committedLimit) > 0;
    }

    public static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String ratio(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }
}
