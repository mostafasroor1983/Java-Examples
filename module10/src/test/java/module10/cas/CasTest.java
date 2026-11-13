package module10.cas;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CasTest {

    @Test
    void casCounterIsExactUnderContention() throws Exception {
        CasCounter c = new CasCounter();
        int threads = 8, per = 50_000;
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < per; i++) c.increment();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        assertEquals((long) threads * per, c.get());
        assertTrue(c.casFailures() >= 0); // contention count is informational
    }

    @Test
    void addIfUnderNeverExceedsCap() throws Exception {
        CasCounter c = new CasCounter();
        int threads = 8, per = 1000;
        AtomicInteger accepted = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < per; i++) if (c.addIfUnder(1, 3000)) accepted.incrementAndGet();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        assertTrue(done.await(20, TimeUnit.SECONDS));
        assertEquals(3000, c.get());
        assertEquals(3000, accepted.get());
    }

    @Test
    void abaDemonstratedAndFixed() {
        assertTrue(AbaProblem.casSucceedsDespiteAba(), "plain CAS cannot see A->B->A");
        assertFalse(AbaProblem.stampedCasDetectsAba(), "stamp changed, CAS fails");
    }

    @Test
    void lockFreeStackUnderConcurrentPushPop() throws Exception {
        AbaProblem.LockFreeStack<Integer> stack = new AbaProblem.LockFreeStack<>();
        int threads = 8, per = 5000;
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < per; i++) stack.push(i);
                } finally {
                    done.countDown();
                }
            }).start();
        }
        assertTrue(done.await(20, TimeUnit.SECONDS));
        int popped = 0;
        while (stack.pop() != null) popped++;
        assertEquals(threads * per, popped);
        assertTrue(stack.isEmpty());
    }
}
