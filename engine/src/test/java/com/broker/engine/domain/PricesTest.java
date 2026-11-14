package com.broker.engine.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PricesTest {

    @Test
    void roundTrip() {
        assertEquals(15025, Prices.toTicks("150.25"));
        assertEquals("150.25", Prices.format(15025));
        assertEquals("0.05", Prices.format(5));
    }

    @Test
    void rejectsPrecisionFinerThanOneTick() {
        assertThrows(ArithmeticException.class, () -> Prices.toTicks("150.255"));
    }

    @Test
    void whyNotDouble() {
        assertNotEquals(0.3, 0.1 + 0.2);                     // binary floating point
        assertEquals(Prices.toTicks("0.30"),
                Prices.toTicks("0.10") + Prices.toTicks("0.20"));   // integers are exact
    }


}