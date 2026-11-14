package com.broker.engine.domain;

import java.math.BigDecimal;

public final class Prices {
    public static final int SCALE = 2; // 1 tick = 0.01

    private Prices() {}

    /** "150.25" -> 15025. Throws ArithmeticException if the input is finer than one tick. */
    public static long toTicks(String decimal) {
        return new BigDecimal(decimal).movePointRight(SCALE).longValueExact();
    }

    /** 15025 -> "150.25" */
    public static String format(long ticks) {
        return BigDecimal.valueOf(ticks, SCALE).toPlainString();
    }
}