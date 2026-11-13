package module15.p11batch;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class P11Test {
    @Test
    void flushesBySizeAndOnDemand() {
        List<List<Integer>> batches = new ArrayList<>();
        var bp = new P11BatchProcessor<Integer>(3, batches::add);
        for (int i = 1; i <= 7; i++) bp.add(i);
        assertEquals(List.of(List.of(1, 2, 3), List.of(4, 5, 6)), batches);
        assertEquals(1, bp.pending());
        bp.flush();
        assertEquals(List.of(7), batches.get(2));
        assertEquals(0, bp.pending());
        assertEquals(3, bp.batchesFlushed());
        bp.flush(); // empty flush is a no-op
        assertEquals(3, batches.size());
    }

    @Test
    void failedBatchIsRetained() {
        AtomicBoolean fail = new AtomicBoolean(true);
        List<Integer> delivered = new ArrayList<>();
        var bp = new P11BatchProcessor<Integer>(2, b -> { if (fail.get()) throw new IllegalStateException("sink down"); delivered.addAll(b); });
        bp.add(1);
        assertThrows(IllegalStateException.class, () -> bp.add(2));
        assertEquals(2, bp.pending());
        fail.set(false);
        bp.add(3);
        bp.flush();
        assertEquals(List.of(1, 2, 3), delivered, "order preserved after retry");
    }

    @Test
    void concurrentAddsDeliverEverythingOnce() throws Exception {
        AtomicInteger delivered = new AtomicInteger();
        var bp = new P11BatchProcessor<Integer>(10, b -> delivered.addAndGet(b.size()));
        int threads = 8, per = 500;
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) new Thread(() -> { try { for (int i = 0; i < per; i++) bp.add(i); } finally { done.countDown(); } }).start();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        bp.flush();
        assertEquals(threads * per, delivered.get());
    }
}
