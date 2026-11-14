package com.broker.engine.book;

import java.util.Iterator;
import java.util.LinkedHashMap;

/** All resting orders at one price, in arrival order (time priority). */
final class PriceLevel {
    private final long priceTicks;
    private final LinkedHashMap<Long, RestingOrder> orders = new LinkedHashMap<>();
    private long totalQuantity;

    PriceLevel(long priceTicks) { this.priceTicks = priceTicks; }

    long priceTicks() { return priceTicks; }
    long totalQuantity() { return totalQuantity; }
    int orderCount() { return orders.size(); }
    boolean isEmpty() { return orders.isEmpty(); }

    void add(RestingOrder r) {
        orders.put(r.order().id(), r);
        totalQuantity += r.remaining();
    }

    boolean remove(long orderId) {
        RestingOrder removed = orders.remove(orderId);
        if (removed == null) return false;
        totalQuantity -= removed.remaining();
        return true;
    }

    /** The order with time priority, or null if the level is empty. */
    RestingOrder first() {
        Iterator<RestingOrder> it = orders.values().iterator();
        return it.hasNext() ? it.next() : null;
    }

    /** Reduces a resting order by qty; removes it from the queue once fully filled. */
    void fill(RestingOrder r, long qty) {
        r.fill(qty);
        totalQuantity -= qty;
        if (r.remaining() == 0) orders.remove(r.order().id());
    }
}