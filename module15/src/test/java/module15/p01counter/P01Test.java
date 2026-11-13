package module15.p01counter;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class P01Test {
    @Test
    void exactUnderContentionAndNeverNegative() throws Exception {
        P01ThreadSafeCounter c = new P01ThreadSafeCounter();
        int threads = 8, per = 20_000;
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) new Thread(() -> { try { for (int i = 0; i < per; i++) c.increment(); } finally { done.countDown(); } }).start();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        assertEquals((long) threads * per, c.get());
        assertEquals((long) threads * per, c.getAndReset());
        assertEquals(0, c.get());
        assertFalse(c.decrement());
        c.increment();
        assertTrue(c.decrement());
        assertEquals(0, c.get());
    }
}
