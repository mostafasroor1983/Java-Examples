package module10.jmm;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PublicationTest {

    @Test
    void safePublicationVariantsAreVisibleToReader() throws Exception {
        SafePublication pub = new SafePublication();
        SafePublication.FxRate rate = new SafePublication.FxRate("GBPUSD", new BigDecimal("1.25"));
        CountDownLatch published = new CountDownLatch(1);
        Thread writer = new Thread(() -> {
            pub.publishVolatile(rate);
            pub.publishAtomic(rate);
            pub.publishLocked(rate);
            published.countDown();
        });
        writer.start();
        assertTrue(published.await(5, TimeUnit.SECONDS));
        assertSame(rate, pub.readVolatile());
        assertSame(rate, pub.readAtomic());
        assertSame(rate, pub.readLocked());
    }

    @Test
    void finalFieldsAreSafeAndDefensivelyCopied() {
        long[] history = {1, 2, 3};
        SafePublication.ImmutableQuote q = new SafePublication.ImmutableQuote(BigDecimal.ONE, history);
        history[0] = 99;
        assertEquals(1, q.history()[0]);
        q.history()[1] = 42;
        assertEquals(2, q.history()[1]);
    }

    @Test
    void unsafePublicationCompilesAndWorksSingleThreaded() {
        // Single-threaded it "works"; multi-threaded visibility is unspecified by the JMM - that is the point.
        UnsafePublication u = new UnsafePublication();
        u.publish(new UnsafePublication.FxRate("EURUSD", BigDecimal.ONE));
        assertEquals("EURUSD", u.read().pair());
    }

    @Test
    void volatileDoubleCheckedLockingInitialisesOnce() throws Exception {
        AtomicInteger constructed = new AtomicInteger();
        DoubleCheckedLocking.VolatileLazy<List<String>> lazy = new DoubleCheckedLocking.VolatileLazy<>(() -> {
            constructed.incrementAndGet();
            return List.of("fee-table");
        });
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    assertEquals(List.of("fee-table"), lazy.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(1, constructed.get());
        assertEquals(1, lazy.factoryCalls());
        assertEquals("1.5%", DoubleCheckedLocking.FeeTableHolder.table().get("CARD"));
        assertNotNull(new DoubleCheckedLocking.BrokenLazy<>(() -> "x").get());
    }
}
