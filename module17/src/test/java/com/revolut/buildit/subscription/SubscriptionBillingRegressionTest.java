package com.revolut.buildit.subscription;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.revolut.buildit.subscription.SubscriptionService.Plan.PLUS;
import static com.revolut.buildit.subscription.SubscriptionService.Status.CANCELLED;
import static org.assertj.core.api.Assertions.assertThat;

/** Regression tests for billing bugs found in review. */
class SubscriptionBillingRegressionTest {
    private final AtomicReference<LocalDate> today = new AtomicReference<>(LocalDate.of(2024, 1, 1));
    private final SubscriptionService svc = new SubscriptionService(today::get);

    @Test
    void cancelledSubscriptionIsNeverChargedOrReactivatedAgain() {
        svc.subscribe("c1", PLUS);              // charge #1
        svc.cancel("c1");
        today.set(LocalDate.of(2024, 2, 1));
        svc.runBilling();                        // finalises the cancellation, no charge
        assertThat(svc.find("c1").orElseThrow().status()).isEqualTo(CANCELLED);
        today.set(LocalDate.of(2024, 3, 1));
        svc.runBilling();                        // BUG before the fix: charged a renewal and set ACTIVE
        assertThat(svc.find("c1").orElseThrow().status()).isEqualTo(CANCELLED);
        assertThat(svc.charges("c1")).hasSize(1);
    }

    @Test
    void concurrentBillingRunsChargeEachDueSubscriptionOnce() throws Exception {
        for (int i = 0; i < 200; i++) svc.subscribe("c" + i, PLUS);
        today.set(LocalDate.of(2024, 2, 1));
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        for (int t = 0; t < 4; t++) pool.submit(() -> { start.await(); return svc.runBilling(); });
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        for (int i = 0; i < 200; i++) {
            assertThat(svc.charges("c" + i)).as("initial charge + exactly one renewal for c" + i).hasSize(2);
        }
    }
}
