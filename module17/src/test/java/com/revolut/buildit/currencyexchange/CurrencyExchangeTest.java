package com.revolut.buildit.currencyexchange;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class CurrencyExchangeTest {
    private CurrencyExchange fx() {
        CurrencyExchange fx = new CurrencyExchange(new BigDecimal("50")); // 0.5% spread
        fx.publishRates(Map.of("GBP", new BigDecimal("1.25"), "EUR", new BigDecimal("1.10")));
        return fx;
    }

    @Test
    void quotesApplySpreadAndRoundDown() {
        CurrencyExchange fx = fx();
        var q = fx.quote("GBP", "USD", new BigDecimal("100")).orElseThrow();
        assertThat(q.rate()).isEqualTo(new BigDecimal("1.24375000")); // 1.25 * (1 - 0.005)
        assertThat(q.result()).isEqualTo(new BigDecimal("124.37"));    // 124.375 rounded DOWN
        var cross = fx.quote("GBP", "EUR", new BigDecimal("100")).orElseThrow();
        assertThat(cross.rate()).isEqualTo(new BigDecimal("1.13068182")); // 1.25/1.10 * 0.995
        assertThat(fx.quote("GBP", "JPY", BigDecimal.ONE)).isEmpty();
        assertThat(fx.quote("GBP", "GBP", BigDecimal.ONE)).isEmpty();
        assertThat(fx.quote("GBP", "USD", BigDecimal.ZERO)).isEmpty();
        assertThatThrownBy(() -> fx.publishRates(Map.of("X", BigDecimal.ZERO))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exchangeMovesBetweenPocketsAndRejectsStaleQuotes() {
        CurrencyExchange fx = fx();
        fx.deposit("c1", "GBP", new BigDecimal("100"));
        var q = fx.quote("GBP", "USD", new BigDecimal("40")).orElseThrow();
        assertThat(fx.exchange("c1", q)).isEqualTo(CurrencyExchange.Outcome.COMPLETED);
        assertThat(fx.balance("c1", "GBP")).isEqualTo(new BigDecimal("60"));
        assertThat(fx.balance("c1", "USD")).isEqualTo(new BigDecimal("49.75"));
        var tooBig = fx.quote("GBP", "USD", new BigDecimal("500")).orElseThrow();
        assertThat(fx.exchange("c1", tooBig)).isEqualTo(CurrencyExchange.Outcome.INSUFFICIENT_FUNDS);
        var old = fx.quote("GBP", "USD", BigDecimal.ONE).orElseThrow();
        fx.publishRates(Map.of("GBP", new BigDecimal("1.30")));
        assertThat(fx.exchange("c1", old)).isEqualTo(CurrencyExchange.Outcome.STALE_QUOTE);
        assertThat(fx.currentRateVersion()).isEqualTo(2);
        assertThat(fx.balance("c1", "GBP")).as("rejected exchanges move nothing").isEqualTo(new BigDecimal("60"));
        assertThatThrownBy(() -> fx.deposit("c1", "GBP", BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentExchangesNeverOverdrawAPocket() throws Exception {
        CurrencyExchange fx = fx();
        fx.deposit("c1", "GBP", new BigDecimal("100"));
        var q = fx.quote("GBP", "USD", BigDecimal.ONE).orElseThrow();
        AtomicInteger ok = new AtomicInteger();
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) new Thread(() -> { try { start.await(); for (int i = 0; i < 50; i++) if (fx.exchange("c1", q) == CurrencyExchange.Outcome.COMPLETED) ok.incrementAndGet(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } finally { done.countDown(); } }).start();
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(ok.get()).isEqualTo(100);
        assertThat(fx.balance("c1", "GBP")).isEqualTo(new BigDecimal("0"));
        assertThat(fx.balance("c1", "USD")).isEqualTo(new BigDecimal("1.24").multiply(BigDecimal.valueOf(100)));
    }
}
