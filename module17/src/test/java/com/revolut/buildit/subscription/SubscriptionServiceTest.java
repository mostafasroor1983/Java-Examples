package com.revolut.buildit.subscription;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static com.revolut.buildit.subscription.SubscriptionService.Plan.*;
import static com.revolut.buildit.subscription.SubscriptionService.Status.*;
import static org.assertj.core.api.Assertions.*;

class SubscriptionServiceTest {
    private final AtomicReference<LocalDate> today = new AtomicReference<>(LocalDate.of(2024, 1, 1));
    private final SubscriptionService svc = new SubscriptionService(today::get);

    @Test
    void subscribeChangePlanProratesAndBills() {
        svc.subscribe("c1", PLUS);
        assertThat(svc.charges("c1")).extracting(SubscriptionService.Charge::amount).containsExactly(new BigDecimal("3.99"));
        assertThatThrownBy(() -> svc.subscribe("c1", METAL)).isInstanceOf(IllegalStateException.class);

        today.set(LocalDate.of(2024, 1, 16)); // 16 of 31 days remain
        svc.changePlan("c1", PREMIUM); // (7.99-3.99) * 16/31 = 2.06
        assertThat(svc.charges("c1").get(1).amount()).isEqualTo(new BigDecimal("2.06"));
        assertThat(svc.find("c1").get().plan()).isEqualTo(PREMIUM);
        svc.changePlan("c1", PREMIUM); // no-op
        assertThat(svc.charges("c1")).hasSize(2);

        today.set(LocalDate.of(2024, 1, 31));
        assertThat(svc.runBilling()).as("not yet due").isZero();
        today.set(LocalDate.of(2024, 2, 1));
        assertThat(svc.runBilling()).isEqualTo(1);
        var s = svc.find("c1").get();
        assertThat(s.periodStart()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(s.nextBillingDate()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(svc.charges("c1").get(2)).extracting(SubscriptionService.Charge::amount, SubscriptionService.Charge::reason)
                .containsExactly(new BigDecimal("7.99"), "renewal PREMIUM");
    }

    @Test
    void downgradeCreditsAndCancellationFinalisesAtPeriodEnd() {
        svc.subscribe("c2", METAL);
        today.set(LocalDate.of(2024, 1, 11)); // 21 of 31 remain
        svc.changePlan("c2", PLUS); // (3.99-14.99)*21/31 = -7.45
        assertThat(svc.charges("c2").get(1).amount()).isEqualTo(new BigDecimal("-7.45"));
        assertThat(svc.cancel("c2").status()).isEqualTo(PENDING_CANCELLATION);
        assertThat(svc.cancel("c2").status()).isEqualTo(PENDING_CANCELLATION);
        today.set(LocalDate.of(2024, 2, 1));
        assertThat(svc.runBilling()).isZero();
        assertThat(svc.find("c2").get().status()).isEqualTo(CANCELLED);
        assertThat(svc.charges("c2")).as("no renewal charge").hasSize(2);
        assertThatThrownBy(() -> svc.changePlan("c2", METAL)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> svc.cancel("ghost")).isInstanceOf(IllegalArgumentException.class);
        svc.subscribe("free", STANDARD);
        assertThat(svc.charges("free")).as("free plan records nothing").isEmpty();
    }
}
