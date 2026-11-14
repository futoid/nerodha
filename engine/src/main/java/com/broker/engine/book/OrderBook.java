package com.broker.engine.book;

import com.broker.engine.domain.Order;
import com.broker.engine.domain.OrderType;
import com.broker.engine.domain.Prices;
import com.broker.engine.domain.Side;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.TreeMap;

/** One symbol's book. NOT thread-safe on purpose: one owner thread (see Week 3). */
public final class OrderBook {
    private final String symbol;
    private final TreeMap<Long, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Long, PriceLevel> asks = new TreeMap<>();
    private final Map<Long, RestingOrder> byId = new HashMap<>();

    public OrderBook(String symbol) {
        this.symbol = symbol;
    }

    public void add(Order order) {
        if (!order.symbol().equals(symbol))
            throw new IllegalArgumentException("wrong symbol: " + order.symbol());
        if (order.type() != OrderType.LIMIT)
            throw new IllegalArgumentException("only limit orders rest in the book");
        if (byId.containsKey(order.id()))
            throw new IllegalArgumentException("duplicate order id " + order.id());

        RestingOrder resting = new RestingOrder(order);
        levels(order.side())
                .computeIfAbsent(order.priceTicks(), PriceLevel::new)
                .add(resting);
        byId.put(order.id(), resting);
    }

    public boolean contains(long orderId) { return byId.containsKey(orderId); }

    public OptionalLong remainingOf(long orderId) {
        RestingOrder r = byId.get(orderId);
        return r == null ? OptionalLong.empty() : OptionalLong.of(r.remaining());
    }

    /** Fills a resting order and cleans up every structure that references it. */
    void fill(RestingOrder resting, long qty) {
        TreeMap<Long, PriceLevel> side = levels(resting.order().side());
        long price = resting.order().priceTicks();
        PriceLevel level = side.get(price);
        level.fill(resting, qty);
        if (resting.remaining() == 0) {
            byId.remove(resting.order().id());
            if (level.isEmpty()) side.remove(price);
        }
    }

    /** Returns true if the order was resting and is now removed. */
    public boolean cancel(long orderId) {
        RestingOrder resting = byId.remove(orderId);
        if (resting == null) return false;

        TreeMap<Long, PriceLevel> side = levels(resting.order().side());
        long price = resting.order().priceTicks();
        PriceLevel level = side.get(price);
        level.remove(orderId);
        if (level.isEmpty()) side.remove(price);   // never leave empty levels behind
        return true;
    }

    public OptionalLong bestBid() {
        return bids.isEmpty() ? OptionalLong.empty() : OptionalLong.of(bids.firstKey());
    }

    public OptionalLong bestAsk() {
        return asks.isEmpty() ? OptionalLong.empty() : OptionalLong.of(asks.firstKey());
    }

    public long quantityAt(Side side, long priceTicks) {
        PriceLevel level = levels(side).get(priceTicks);
        return level == null ? 0 : level.totalQuantity();
    }

    public int orderCount() { return byId.size(); }
    public int levelCount(Side side) { return levels(side).size(); }

    /** The order with price-time priority on that side, or null. Used by matching on Day 10. */
    RestingOrder peekBest(Side side) {
        TreeMap<Long, PriceLevel> m = levels(side);
        return m.isEmpty() ? null : m.firstEntry().getValue().first();
    }

    private TreeMap<Long, PriceLevel> levels(Side side) {
        return side == Side.BUY ? bids : asks;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(symbol).append('\n');
        asks.descendingMap().forEach((p, l) ->
                sb.append("  ASK ").append(Prices.format(p)).append(" x ").append(l.totalQuantity()).append('\n'));
        bids.forEach((p, l) ->
                sb.append("  BID ").append(Prices.format(p)).append(" x ").append(l.totalQuantity()).append('\n'));
        return sb.toString();
    }
}