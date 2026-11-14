package com.broker.engine.book;

import com.broker.engine.domain.Order;
import com.broker.engine.domain.OrderType;
import com.broker.engine.domain.Side;
import com.broker.engine.domain.Trade;
import java.util.ArrayList;
import java.util.List;

/** Matches orders for one symbol. NOT thread-safe: one owner thread (Week 3). */
public final class MatchingEngine {
    private final String symbol;
    private final OrderBook book;
    private long nextTradeId = 1;

    public MatchingEngine(String symbol) {
        this.symbol = symbol;
        this.book = new OrderBook(symbol);
    }

    /** For reads. All changes go through submit() and cancel(). */
    public OrderBook book() { return book; }

    public boolean cancel(long orderId) { return book.cancel(orderId); }

    public List<Trade> submit(Order incoming) {
        if (!incoming.symbol().equals(symbol))
            throw new IllegalArgumentException("wrong symbol: " + incoming.symbol());
        if (book.contains(incoming.id()))
            throw new IllegalArgumentException("duplicate order id " + incoming.id());

        List<Trade> trades = new ArrayList<>();
        Side opposite = incoming.side() == Side.BUY ? Side.SELL : Side.BUY;
        boolean incomingIsBuy = incoming.side() == Side.BUY;
        long remaining = incoming.quantity();

        while (remaining > 0) {
            RestingOrder best = book.peekBest(opposite);
            if (best == null) break;

            long restingPrice = best.order().priceTicks();
            if (incoming.type() == OrderType.LIMIT && !crosses(incoming, restingPrice)) break;

            long qty = Math.min(remaining, best.remaining());
            trades.add(new Trade(
                    nextTradeId++, symbol,
                    incomingIsBuy ? incoming.id() : best.order().id(),
                    incomingIsBuy ? best.order().id() : incoming.id(),
                    restingPrice, qty, incoming.side()));

            book.fill(best, qty);
            remaining -= qty;
        }

        if (remaining > 0 && incoming.type() == OrderType.LIMIT) {
            book.add(remaining == incoming.quantity() ? incoming : incoming.withQuantity(remaining));
        }
        return trades;
    }

    private static boolean crosses(Order incoming, long restingPrice) {
        return incoming.side() == Side.BUY
                ? incoming.priceTicks() >= restingPrice
                : incoming.priceTicks() <= restingPrice;
    }
}