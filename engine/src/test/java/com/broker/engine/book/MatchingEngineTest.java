package com.broker.engine.book;

import static org.junit.jupiter.api.Assertions.*;

import com.broker.engine.domain.Order;
import com.broker.engine.domain.Prices;
import com.broker.engine.domain.Side;
import com.broker.engine.domain.Trade;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MatchingEngineTest {
    private MatchingEngine engine;

    @BeforeEach
    void setUp() { engine = new MatchingEngine("AAPL"); }

    private static Order buy(long id, String px, long qty) {
        return Order.limit(id, 7, "AAPL", Side.BUY, Prices.toTicks(px), qty);
    }

    private static Order sell(long id, String px, long qty) {
        return Order.limit(id, 8, "AAPL", Side.SELL, Prices.toTicks(px), qty);
    }

    @Test
    void noOppositeOrdersMeansTheOrderRests() {
        List<Trade> trades = engine.submit(buy(1, "150.00", 10));
        assertTrue(trades.isEmpty());
        assertEquals(15000, engine.book().bestBid().getAsLong());
        assertEquals(1, engine.book().orderCount());
    }

    @Test
    void exactMatchClearsBothSides() {
        engine.submit(sell(1, "150.00", 100));
        List<Trade> trades = engine.submit(buy(2, "150.00", 100));

        assertEquals(1, trades.size());
        Trade t = trades.get(0);
        assertEquals(100, t.quantity());
        assertEquals(15000, t.priceTicks());
        assertEquals(2, t.buyOrderId());
        assertEquals(1, t.sellOrderId());
        assertEquals(Side.BUY, t.aggressor());
        assertEquals(0, engine.book().orderCount());
    }

    @Test
    void tradesAtTheRestingOrdersPrice() {
        engine.submit(sell(1, "150.00", 10));
        List<Trade> trades = engine.submit(buy(2, "151.00", 10));
        assertEquals(15000, trades.get(0).priceTicks());
    }

    @Test
    void incomingPartiallyFilledRestsItsRemainder() {
        engine.submit(sell(1, "150.00", 50));
        List<Trade> trades = engine.submit(buy(2, "150.00", 100));

        assertEquals(50, trades.get(0).quantity());
        assertTrue(engine.book().bestAsk().isEmpty());
        assertEquals(15000, engine.book().bestBid().getAsLong());
        assertEquals(50, engine.book().remainingOf(2).getAsLong());
    }

    @Test
    void restingOrderPartiallyFilledStaysAtTheFront() {
        engine.submit(sell(1, "150.00", 100));
        engine.submit(buy(2, "150.00", 30));

        assertEquals(70, engine.book().remainingOf(1).getAsLong());
        assertEquals(70, engine.book().quantityAt(Side.SELL, 15000));
        assertEquals(1, engine.book().peekBest(Side.SELL).order().id());
    }

    @Test
    void bestPriceIsMatchedFirst() {
        engine.submit(sell(1, "150.50", 10));
        engine.submit(sell(2, "150.00", 10));
        List<Trade> trades = engine.submit(buy(3, "151.00", 10));

        assertEquals(1, trades.size());
        assertEquals(2, trades.get(0).sellOrderId());
        assertEquals(15000, trades.get(0).priceTicks());
    }

    @Test
    void oldestOrderAtAPriceIsMatchedFirst() {
        engine.submit(sell(1, "150.00", 10));
        engine.submit(sell(2, "150.00", 10));
        List<Trade> trades = engine.submit(buy(3, "150.00", 15));

        assertEquals(2, trades.size());
        assertEquals(1, trades.get(0).sellOrderId());
        assertEquals(10, trades.get(0).quantity());
        assertEquals(2, trades.get(1).sellOrderId());
        assertEquals(5, trades.get(1).quantity());
        assertTrue(engine.book().remainingOf(1).isEmpty());
        assertEquals(5, engine.book().remainingOf(2).getAsLong());
    }

    @Test
    void limitOrderSweepsLevelsUpToItsLimitThenRests() {
        engine.submit(sell(1, "150.00", 10));
        engine.submit(sell(2, "150.25", 10));
        engine.submit(sell(3, "150.50", 10));
        List<Trade> trades = engine.submit(buy(4, "150.25", 25));

        assertEquals(2, trades.size());
        assertEquals(15000, trades.get(0).priceTicks());
        assertEquals(15025, trades.get(1).priceTicks());
        assertEquals(15025, engine.book().bestBid().getAsLong());
        assertEquals(5, engine.book().quantityAt(Side.BUY, 15025));
        assertEquals(15050, engine.book().bestAsk().getAsLong());
    }

    @Test
    void nonCrossingOrdersRestAndBookStaysUncrossed() {
        engine.submit(sell(1, "150.00", 10));
        List<Trade> trades = engine.submit(buy(2, "149.99", 10));

        assertTrue(trades.isEmpty());
        assertTrue(engine.book().bestBid().getAsLong() < engine.book().bestAsk().getAsLong());
    }

    @Test
    void sellSideMatchesTheHighestBidFirst() {
        engine.submit(buy(1, "150.00", 10));
        engine.submit(buy(2, "150.25", 10));
        List<Trade> trades = engine.submit(sell(3, "150.00", 10));

        assertEquals(1, trades.size());
        Trade t = trades.get(0);
        assertEquals(15025, t.priceTicks());
        assertEquals(2, t.buyOrderId());
        assertEquals(3, t.sellOrderId());
        assertEquals(Side.SELL, t.aggressor());
        assertEquals(15000, engine.book().bestBid().getAsLong());
    }

    @Test
    void marketOrderSweepsLevelsAtTheirOwnPrices() {
        engine.submit(sell(1, "150.00", 10));
        engine.submit(sell(2, "151.00", 10));
        List<Trade> trades = engine.submit(Order.market(3, 7, "AAPL", Side.BUY, 15));

        assertEquals(2, trades.size());
        assertEquals(15000, trades.get(0).priceTicks());
        assertEquals(10, trades.get(0).quantity());
        assertEquals(15100, trades.get(1).priceTicks());
        assertEquals(5, trades.get(1).quantity());
        assertEquals(5, engine.book().quantityAt(Side.SELL, 15100));
    }

    @Test
    void marketOrderRemainderIsDiscardedNotRested() {
        engine.submit(sell(1, "150.00", 10));
        List<Trade> trades = engine.submit(Order.market(2, 7, "AAPL", Side.BUY, 25));

        assertEquals(1, trades.size());
        assertEquals(0, engine.book().orderCount());
        assertTrue(engine.book().bestBid().isEmpty());
    }

    @Test
    void marketOrderOnEmptyBookDoesNothing() {
        List<Trade> trades = engine.submit(Order.market(1, 7, "AAPL", Side.SELL, 10));
        assertTrue(trades.isEmpty());
        assertEquals(0, engine.book().orderCount());
    }

    @Test
    void cancelAfterFullFillReturnsFalse() {
        engine.submit(sell(1, "150.00", 10));
        engine.submit(buy(2, "150.00", 10));
        assertFalse(engine.cancel(1));
    }

    @Test
    void duplicateRestingIdIsRejectedBeforeAnyTrade() {
        engine.submit(buy(1, "150.00", 10));
        engine.submit(sell(2, "151.00", 10));

        assertThrows(IllegalArgumentException.class, () -> engine.submit(buy(1, "151.00", 10)));
        assertEquals(2, engine.book().orderCount());
    }

    @Test
    void tradeIdsAreSequential() {
        engine.submit(sell(1, "150.00", 10));
        engine.submit(sell(2, "150.00", 10));
        List<Trade> trades = engine.submit(buy(3, "150.00", 20));

        assertEquals(1, trades.get(0).id());
        assertEquals(2, trades.get(1).id());
    }
}