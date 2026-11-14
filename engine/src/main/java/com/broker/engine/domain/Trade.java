package com.broker.engine.domain;

import java.util.Objects;

public record Trade(long id, String symbol, long buyOrderId, long sellOrderId,
                    long priceTicks, long quantity, Side aggressor) {

    public Trade {
        if (id <= 0) throw new IllegalArgumentException("id must be positive");
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol required");
        if (priceTicks <= 0) throw new IllegalArgumentException("price must be positive");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        Objects.requireNonNull(aggressor, "aggressor");
    }
}