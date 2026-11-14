package com.broker.engine.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void validLimitOrder() {
        Order o = Order.limit(1, 7, "AAPL", Side.BUY, Prices.toTicks("150.25"), 100);
        assertEquals(15025, o.priceTicks());
        assertEquals(OrderType.LIMIT, o.type());
    }

    @Test
    void marketOrderHasNoPrice() {
        Order o = Order.market(2, 7, "AAPL", Side.SELL, 50);
        assertEquals(0, o.priceTicks());
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> Order.limit(1, 7, "AAPL", Side.BUY, 15025, 0));
        assertThrows(IllegalArgumentException.class, () -> Order.limit(1, 7, "AAPL", Side.BUY, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> Order.limit(1, 7, " ", Side.BUY, 15025, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new Order(1, 7, "AAPL", Side.BUY, OrderType.MARKET, 15025, 10));
    }

    @Test
    void notionalOverflowIsDetected() {
        Order o = Order.limit(1, 7, "AAPL", Side.BUY, Long.MAX_VALUE, 2);
        assertThrows(ArithmeticException.class, o::notionalTicks);
    }

    @Test
    void recordsCompareByValue() {
        Order a = Order.limit(1, 7, "AAPL", Side.BUY, 15025, 10);
        Order b = Order.limit(1, 7, "AAPL", Side.BUY, 15025, 10);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}