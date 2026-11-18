package module15.p05idempotency;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class P05Test {
    private static P05IdempotentPaymentService.Request req(String key, String amt) {
        return new P05IdempotentPaymentService.Request(key, "A", new BigDecimal(amt));
    }

    @Test
    void sameKeySamePayloadReturnsSameResponse() {
        var svc = new P05IdempotentPaymentService();
        var r1 = svc.submit(req("k1", "10"));
        var r2 = svc.submit(req("k1", "10"));
        assertEquals(r1, r2);
        assertEquals(1, svc.executions());
        assertThrows(P05IdempotentPaymentService.ConflictException.class, () -> svc.submit(req("k1", "11")));
        assertNotEquals(r1, svc.submit(req("k2", "10")));
        assertEquals(r1, svc.submit(req("k1", "10.00")), "same amount with a different scale is NOT a conflict");
        assertEquals(2, svc.executions(), "k1 once and k2 once; the k1 retry did not execute again");
    }

    @Test
    void failureAllowsRetry() {
        var svc = new P05IdempotentPaymentService();
        assertThrows(IllegalArgumentException.class, () -> svc.submit(req("bad", "0")));
        assertEquals("ACCEPTED", svc.submit(req("bad", "5")).status(), "key freed after failure");
    }

    @Test
    void concurrentDuplicatesExecuteOnce() throws Exception {
        var svc = new P05IdempotentPaymentService();
        int threads = 32;
        var responses = new P05IdempotentPaymentService.Response[threads];
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            int i = t;
            new Thread(() -> {
                try {
                    start.await();
                    responses[i] = svc.submit(req("shared", "10"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        assertEquals(1, svc.executions());
        for (var r : responses) assertEquals(responses[0], r);
    }
}
