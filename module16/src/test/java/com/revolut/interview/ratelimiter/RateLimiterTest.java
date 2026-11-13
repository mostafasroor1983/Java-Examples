package com.revolut.interview.ratelimiter;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

class RateLimiterTest {

    @Test
    void fixedWindowAllowsBoundaryBurst() {
        AtomicLong ms = new AtomicLong(0);
        FixedWindowRateLimiter rl = new FixedWindowRateLimiter(3, 1000, ms::get);
        ms.set(900);
        assertThat(rl.allow("k")).isTrue();
        assertThat(rl.allow("k")).isTrue();
        assertThat(rl.allow("k")).isTrue();
        assertThat(rl.allow("k")).isFalse();
        ms.set(1000); // new window: 3 more allowed within 100ms of the previous 3 -> 6 in 100ms
        assertThat(rl.allow("k")).isTrue();
        assertThat(rl.allow("other")).as("keys independent").isTrue();
        ms.set(5000);
        assertThat(rl.purge()).as("windows (k,0), (k,1), (other,1) are all stale").isEqualTo(3);
    }

    @Test
    void slidingWindowIsExactAcrossBoundary() {
        AtomicLong ms = new AtomicLong(900);
        SlidingWindowRateLimiter rl = new SlidingWindowRateLimiter(3, 1000, ms::get);
        for (long t : new long[]{900, 950, 999}) {
            ms.set(t);
            assertThat(rl.allow("k")).isTrue();
        }
        ms.set(1000);
        assertThat(rl.allow("k")).as("still 3 in the last second").isFalse();
        ms.set(1900);
        assertThat(rl.allow("k")).as("t=900 has slid out, 950 and 999 remain").isTrue();
        assertThat(rl.allow("k")).isFalse();
    }

    @Test
    void tokenBucketBurstsThenSmooths() {
        AtomicLong nanos = new AtomicLong();
        TokenBucketRateLimiter rl = new TokenBucketRateLimiter(2, 1.0, nanos::get); // 1 token/s
        assertThat(rl.allow("k")).isTrue();
        assertThat(rl.allow("k")).isTrue();
        assertThat(rl.allow("k")).isFalse();
        nanos.addAndGet(500_000_000L);
        assertThat(rl.allow("k")).as("half a token").isFalse();
        nanos.addAndGet(500_000_000L);
        assertThat(rl.allow("k")).isTrue();
        nanos.addAndGet(60_000_000_000L);
        assertThat(rl.allow("k")).isTrue();
        assertThat(rl.allow("k")).isTrue();
        assertThat(rl.allow("k")).as("capped at capacity 2").isFalse();
        assertThatThrownBy(() -> new TokenBucketRateLimiter(0, 1, nanos::get)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FixedWindowRateLimiter(1, 0, nanos::get)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allThreeNeverExceedLimitUnderConcurrency() throws Exception {
        RateLimiter[] limiters = {
                new FixedWindowRateLimiter(100, 60_000, () -> 0L),
                new SlidingWindowRateLimiter(100, 60_000, () -> 0L),
                new TokenBucketRateLimiter(100, 0.0000001, () -> 0L)};
        for (RateLimiter rl : limiters) {
            AtomicInteger allowed = new AtomicInteger();
            int threads = 16;
            CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
            for (int t = 0; t < threads; t++) {
                new Thread(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 50; i++) if (rl.allow("shared")) allowed.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }).start();
            }
            start.countDown();
            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(allowed.get()).as(rl.getClass().getSimpleName()).isEqualTo(100);
        }
    }
}
