package com.creditsentinel.domain;

/**
 * Internal demo scale, strongest first. Investment grade is AAA through BBB.
 * This is not a live S&P or Moody's rating.
 */
public enum Rating {
    AAA,
    AA,
    A,
    BBB,
    BB,
    B,
    CCC;

    public boolean isInvestmentGrade() {
        return ordinal() <= BBB.ordinal();
    }

    public boolean isWorseThan(Rating other) {
        return ordinal() > other.ordinal();
    }
}
