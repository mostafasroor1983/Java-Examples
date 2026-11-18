package module10.snapshot;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class QuoteBookTest {

    @Test
    void snapshotsAreImmutableAndVersioned() {
        var book = new QuoteBook();
        var s1 = book.publish(Map.of("EURUSD", new QuoteBook.Quote(1_085_000, 1_085_200)), Instant.EPOCH);
        var s2 = book.publish(Map.of("GBPUSD", new QuoteBook.Quote(1_267_000, 1_267_300)), Instant.EPOCH.plusSeconds(1));
        assertEquals(1, s1.version());
        assertEquals(2, s2.version());
        assertEquals(1, s1.quotes().size(), "old snapshot unaffected by later publish");
        assertEquals(2, book.snapshot().quotes().size());
        assertThrows(UnsupportedOperationException.class, () -> s2.quotes().put("X", new QuoteBook.Quote(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> new QuoteBook.Quote(10, 9));
        assertTrue(book.quote("USDJPY").isEmpty());
    }

    @Test
    void readersNeverSeeACrossedOrMixedQuote() throws Exception {
        var book = new QuoteBook();
        AtomicBoolean stop = new AtomicBoolean();
        AtomicInteger violations = new AtomicInteger();
        AtomicLong reads = new AtomicLong();
        Thread[] readers = new Thread[4];
        for (int r = 0; r < readers.length; r++) {
            readers[r] = Thread.ofPlatform().start(() -> {
                while (!stop.get()) {
                    var s = book.snapshot();
                    var a = s.quotes().get("A");
                    var b = s.quotes().get("B");
                    // writer always publishes A and B with the SAME mid encoded in the bid; any mismatch = torn view
                    if (a != null && (b == null || a.bidMicros() != b.bidMicros())) violations.incrementAndGet();
                    reads.incrementAndGet();
                }
            });
        }
        for (long i = 1; i <= 20_000; i++) {
            book.publish(Map.of("A", new QuoteBook.Quote(i, i + 5), "B", new QuoteBook.Quote(i, i + 7)), Instant.EPOCH);
        }
        stop.set(true);
        for (Thread t : readers) t.join();
        assertEquals(0, violations.get());
        assertEquals(20_000, book.snapshot().version());
        assertTrue(reads.get() > 0);
    }

    @Test
    void tornQuoteCanBeObservedButIsNeverAssertedAway() throws Exception {
        // Documentation test: the bad version permits bid > ask. We only check it compiles and runs; whether a torn
        // read is observed depends on timing, which is exactly why such bugs escape testing.
        var q = new TornQuote();
        q.update(100, 101);
        assertArrayEquals(new long[]{100, 101}, q.read());
    }
}
