package com.broker.engine.domain;

import java.util.Objects;

public record Order(long id, long accountId, String symbol, Side side,
                    OrderType type, long priceTicks, long quantity) {

    public Order {
        if (id <= 0) throw new IllegalArgumentException("id must be positive");
        if (accountId <= 0) throw new IllegalArgumentException("accountId must be positive");
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol required");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(type, "type");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (type == OrderType.LIMIT && priceTicks <= 0)
            throw new IllegalArgumentException("limit order needs a positive price");
        if (type == OrderType.MARKET && priceTicks != 0)
            throw new IllegalArgumentException("market order must not carry a price");
    }

    public static Order limit(long id, long accountId, String symbol, Side side,
                              long priceTicks, long quantity) {
        return new Order(id, accountId, symbol, side, OrderType.LIMIT, priceTicks, quantity);
    }

    public static Order market(long id, long accountId, String symbol, Side side, long quantity) {
        return new Order(id, accountId, symbol, side, OrderType.MARKET, 0, quantity);
    }

    /** price x quantity, in ticks. Overflow throws instead of silently wrapping. */
    public long notionalTicks() {
        if (type == OrderType.MARKET) throw new IllegalStateException("market order has no price");
        return Math.multiplyExact(priceTicks, quantity);
    }
}