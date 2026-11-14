package com.broker.engine.book;

import static org.junit.jupiter.api.Assertions.*;

import com.broker.engine.domain.Order;
import com.broker.engine.domain.Prices;
import com.broker.engine.domain.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderBookTest {
    private OrderBook book;

    @BeforeEach
    void setUp() { book = new OrderBook("AAPL"); }

    private static Order buy(long id, String px, long qty) {
        return Order.limit(id, 7, "AAPL", Side.BUY, Prices.toTicks(px), qty);
    }

    private static Order sell(long id, String px, long qty) {
        return Order.limit(id, 8, "AAPL", Side.SELL, Prices.toTicks(px), qty);
    }

    @Test
    void emptyBookHasNoBestPrices() {
        assertTrue(book.bestBid().isEmpty());
        assertTrue(book.bestAsk().isEmpty());
        assertEquals(0, book.orderCount());
        assertNull(book.peekBest(Side.BUY));
    }

    @Test
    void bestBidIsHighestAndBestAskIsLowest() {
        book.add(buy(1, "150.00", 10));
        book.add(buy(2, "150.25", 10));
        book.add(buy(3, "149.75", 10));
        book.add(sell(4, "150.75", 10));
        book.add(sell(5, "150.50", 10));

        assertEquals(15025, book.bestBid().getAsLong());
        assertEquals(15050, book.bestAsk().getAsLong());
    }

    @Test
    void sameLevelKeepsArrivalOrder() {
        book.add(buy(1, "150.00", 10));
        book.add(buy(2, "150.00", 10));
        book.add(buy(3, "150.00", 10));

        assertEquals(1, book.peekBest(Side.BUY).order().id());
    }

    @Test
    void cancelInTheMiddlePreservesOrderOfTheRest() {
        book.add(buy(1, "150.00", 10));
        book.add(buy(2, "150.00", 10));
        book.add(buy(3, "150.00", 10));

        assertTrue(book.cancel(2));
        assertEquals(1, book.peekBest(Side.BUY).order().id());

        assertTrue(book.cancel(1));
        assertEquals(3, book.peekBest(Side.BUY).order().id());
    }

    @Test
    void cancellingLastOrderRemovesTheLevel() {
        book.add(buy(1, "150.25", 10));
        book.add(buy(2, "150.00", 10));

        assertTrue(book.cancel(1));

        assertEquals(15000, book.bestBid().getAsLong());
        assertEquals(1, book.levelCount(Side.BUY));
    }

    @Test
    void cancelUnknownOrAlreadyCancelledReturnsFalse() {
        book.add(buy(1, "150.00", 10));
        assertFalse(book.cancel(99));
        assertTrue(book.cancel(1));
        assertFalse(book.cancel(1));
    }

    @Test
    void quantityAtSumsOneLevelOnOneSide() {
        book.add(buy(1, "150.25", 100));
        book.add(buy(2, "150.25", 50));
        book.add(sell(3, "150.25", 30));

        assertEquals(150, book.quantityAt(Side.BUY, 15025));
        assertEquals(30, book.quantityAt(Side.SELL, 15025));
        assertEquals(0, book.quantityAt(Side.BUY, 15000));
    }

    @Test
    void rejectsInvalidAdds() {
        book.add(buy(1, "150.00", 10));
        assertThrows(IllegalArgumentException.class, () -> book.add(buy(1, "151.00", 10)));
        assertThrows(IllegalArgumentException.class,
                () -> book.add(Order.market(2, 7, "AAPL", Side.BUY, 10)));
        assertThrows(IllegalArgumentException.class,
                () -> book.add(Order.limit(3, 7, "TSLA", Side.BUY, 15000, 10)));
    }
}