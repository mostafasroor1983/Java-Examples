package com.revolut.buildit.ratelimiter;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.revolut.buildit.ratelimiter.TieredRateLimiter.Tier.*;
import static org.assertj.core.api.Assertions.*;

class TieredRateLimiterTest {
    private final AtomicLong ms = new AtomicLong(0);
    private final TieredRateLimiter rl = new TieredRateLimiter(Map.of(FREE, 2, PRO, 10, ENTERPRISE, 100), 1000, 50, ms::get);

    @Test
    void perTierLimitsRemainingAndRetryAfter() {
        assertThat(rl.check("f", FREE)).extracting(TieredRateLimiter.Decision::allowed, TieredRateLimiter.Decision::remaining).containsExactly(true, 1L);
        assertThat(rl.check("f", FREE).remaining()).isZero();
        var denied = rl.check("f", FREE);
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterMillis()).isEqualTo(1000);
        assertThat(rl.check("p", PRO).allowed()).as("different client/tier unaffected").isTrue();
        assertThatThrownBy(() -> new TieredRateLimiter(Map.of(), 1000, 10, ms::get).check("x", FREE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void slidingCounterWeighsPreviousWindow() {
        for (int i = 0; i < 10; i++) assertThat(rl.check("p", PRO).allowed()).isTrue();
        ms.set(1000); // new window, previous fully counted => denied
        assertThat(rl.check("p", PRO).allowed()).isFalse();
        ms.set(1500); // half the previous window has slid out: estimate = 10*0.5 = 5 -> 5 more allowed
        int allowed = 0;
        for (int i = 0; i < 10; i++) if (rl.check("p", PRO).allowed()) allowed++;
        assertThat(allowed).isEqualTo(5);
        ms.set(5000); // windows skipped entirely => fresh
        assertThat(rl.check("p", PRO).remaining()).isEqualTo(9);
    }

    @Test
    void globalCeilingAppliesAcrossClients() throws Exception {
        AtomicInteger allowed = new AtomicInteger();
        int threads = 10;
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            String client = "e" + t;
            new Thread(() -> { try { start.await(); for (int i = 0; i < 20; i++) if (rl.check(client, ENTERPRISE).allowed()) allowed.incrementAndGet(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } finally { done.countDown(); } }).start();
        }
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(allowed.get()).as("200 requested, global limit 50").isEqualTo(50);
    }
}
