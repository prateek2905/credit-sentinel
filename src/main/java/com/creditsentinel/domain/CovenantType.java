package com.creditsentinel.domain;

public enum CovenantType {
    /** Cash flow divided by debt payments. Higher is safer. Fails when actual is below the minimum. */
    MIN_DSCR,
    /** Debt divided by earnings. Higher is riskier. Fails when actual is above the maximum. */
    MAX_LEVERAGE
}
