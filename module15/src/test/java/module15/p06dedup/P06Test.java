package module15.p06dedup;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class P06Test {
    @Test
    void dedupWithinWindowThenAcceptsAgain() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        var d = new P06EventDeduplicator(Duration.ofMinutes(5), now::get);
        assertTrue(d.firstSeen("evt-1"));
        assertFalse(d.firstSeen("evt-1"));
        now.set(Instant.EPOCH.plusSeconds(301));
        assertTrue(d.firstSeen("evt-1"), "window expired");
        d.firstSeen("evt-2");
        now.set(Instant.EPOCH.plusSeconds(700));
        assertEquals(2, d.purgeExpired());
        assertEquals(0, d.size());
    }

    @Test
    void exactlyOneWinnerUnderConcurrency() throws Exception {
        var d = new P06EventDeduplicator(Duration.ofMinutes(1), Instant::now);
        AtomicInteger winners = new AtomicInteger();
        int threads = 32;
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    start.await();
                    if (d.firstSeen("webhook-77")) winners.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        assertEquals(1, winners.get());
    }
}
