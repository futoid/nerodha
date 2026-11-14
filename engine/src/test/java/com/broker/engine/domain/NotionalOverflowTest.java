package com.broker.engine.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NotionalOverflowTest {

    @Test
    void maxPriceTimesOneSurvives() {
        Order o = Order.limit(1, 7, "AAPL", Side.BUY, Long.MAX_VALUE, 1);
        assertEquals(Long.MAX_VALUE, o.notionalTicks());
    }

    @Test
    void maxPriceTimesTwoIsDetected() {
        Order o = Order.limit(1, 7, "AAPL", Side.BUY, Long.MAX_VALUE, 2);
        assertThrows(ArithmeticException.class, o::notionalTicks);
    }

    @Test
    void plainMultiplicationWouldHaveFailedSilently() {
        // What notionalTicks() would return if it used `*` instead of multiplyExact
        long wrapped = Long.MAX_VALUE * 2;
        assertEquals(-2L, wrapped);   // a huge positive order becomes a tiny negative number
    }
}