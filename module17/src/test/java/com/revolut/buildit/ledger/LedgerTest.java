package com.revolut.buildit.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class LedgerTest {
    private final Ledger ledger = new Ledger(() -> Instant.EPOCH);

    @Test
    void doubleEntryBalancesAndIdempotency() {
        assertThat(ledger.transfer("t1", "cash", "alice", "GBP", new BigDecimal("100"))).isTrue();
        assertThat(ledger.transfer("t1", "cash", "alice", "GBP", new BigDecimal("100"))).as("duplicate id ignored").isFalse();
        assertThat(ledger.balance("alice", "GBP")).isEqualTo(new BigDecimal("100"));
        assertThat(ledger.balance("cash", "GBP")).isEqualTo(new BigDecimal("-100"));
        assertThat(ledger.systemTotal("GBP")).isZero();
        assertThat(ledger.journal()).hasSize(1);
        assertThat(ledger.reconcile()).isEmpty();
    }

    @Test
    void unbalancedAndInvalidEntriesAreRejected() {
        assertThatThrownBy(() -> ledger.post("bad", "oops", List.of(
                new Ledger.Posting("a", "GBP", new BigDecimal("10")),
                new Ledger.Posting("b", "GBP", new BigDecimal("-9")))))
                .isInstanceOf(Ledger.UnbalancedEntryException.class).hasMessageContaining("GBP");
        assertThatThrownBy(() -> ledger.post("one", "single", List.of(new Ledger.Posting("a", "GBP", BigDecimal.ONE)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Ledger.Posting("a", "GBP", BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ledger.transfer("neg", "a", "b", "GBP", new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(ledger.journal()).as("nothing was appended").isEmpty();
        // multi-currency entry: must balance per currency
        assertThat(ledger.post("fx", "fx trade", List.of(
                new Ledger.Posting("alice", "GBP", new BigDecimal("-80")),
                new Ledger.Posting("fxdesk", "GBP", new BigDecimal("80")),
                new Ledger.Posting("fxdesk", "USD", new BigDecimal("-100")),
                new Ledger.Posting("alice", "USD", new BigDecimal("100"))))).isTrue();
        assertThat(ledger.systemTotal("GBP")).isZero();
        assertThat(ledger.systemTotal("USD")).isZero();
    }

    @Test
    void concurrentPostingsKeepSystemBalancedAndReconcile() throws Exception {
        String[] accounts = {"a", "b", "c", "d"};
        int threads = 8, per = 500;
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            int id = t;
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < per; i++) ledger.transfer("t" + id + "-" + i, accounts[i % 4], accounts[(i + 1) % 4], "GBP", BigDecimal.ONE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertThat(done.await(20, TimeUnit.SECONDS)).isTrue();
        assertThat(ledger.journal()).hasSize(threads * per);
        assertThat(ledger.systemTotal("GBP")).isZero();
        assertThat(ledger.reconcile()).isEmpty();
        assertThatThrownBy(() -> ledger.journal().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
