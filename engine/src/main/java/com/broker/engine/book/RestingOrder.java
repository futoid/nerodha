package com.broker.engine.book;

import com.broker.engine.domain.Order;

/** Wraps an immutable Order with mutable remaining quantity. Owned by one thread only. */
final class RestingOrder {
    private final Order order;
    private long remaining;

    RestingOrder(Order order) {
        this.order = order;
        this.remaining = order.quantity();
    }

    Order order() { return order; }
    long remaining() { return remaining; }
}