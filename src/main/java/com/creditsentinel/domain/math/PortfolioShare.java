package com.creditsentinel.domain.math;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PortfolioShare {

    private PortfolioShare() {
    }

    public static BigDecimal share(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.UNNECESSARY);
        }
        return part.divide(total, 4, RoundingMode.HALF_UP);
    }

    public static boolean aboveCap(BigDecimal share, BigDecimal cap) {
        return share.compareTo(cap) > 0;
    }
}
