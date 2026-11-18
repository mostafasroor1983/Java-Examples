package com.revolut.buildit.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class LedgerReversalTest {
    private final Ledger ledger = new Ledger(() -> Instant.EPOCH);

    @Test
    void reversalAppendsNegatedEntryAndKeepsHistory() {
        ledger.transfer("t1", "cash", "alice", "GBP", new BigDecimal("100"));
        assertThat(ledger.reverse("r1", "t1", "sent to wrong customer")).isTrue();
        assertThat(ledger.balance("alice", "GBP")).isZero();
        assertThat(ledger.journal()).hasSize(2).extracting(Ledger.JournalEntry::id).containsExactly("t1", "r1");
        assertThat(ledger.journal().get(1).description()).contains("reversal of t1").contains("wrong customer");
        assertThat(ledger.reconcile()).isEmpty();
        assertThat(ledger.systemTotal("GBP")).isZero();
    }

    @Test
    void reversalRulesAndIdempotency() {
        ledger.transfer("t1", "cash", "alice", "GBP", BigDecimal.TEN);
        ledger.transfer("t2", "cash", "bob", "GBP", BigDecimal.ONE);
        assertThat(ledger.reverse("r1", "t1", "dup")).isTrue();
        assertThat(ledger.reverse("r1-again", "t1", "dup")).as("already reversed").isFalse();
        assertThatThrownBy(() -> ledger.reverse("rr", "r1", "undo")).hasMessageContaining("cannot reverse a reversal");
        assertThatThrownBy(() -> ledger.reverse("rx", "missing", "x")).hasMessageContaining("unknown entry");
        assertThatThrownBy(() -> ledger.reverse("rx", "t2", " ")).hasMessageContaining("reason");
        assertThatThrownBy(() -> ledger.reverse("t1", "t2", "id clash")).isInstanceOf(IllegalStateException.class);
        assertThat(ledger.reverse("r2", "t2", "retry after clash")).as("claim released after the clash").isTrue();
    }

    @Test
    void concurrentReversalsOfSameEntryPostOnce() throws Exception {
        ledger.transfer("t1", "cash", "alice", "GBP", new BigDecimal("50"));
        AtomicInteger posted = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < 32; i++) {
            int n = i;
            pool.submit(() -> {
                start.await();
                if (ledger.reverse("r" + n, "t1", "ops ticket 42")) posted.incrementAndGet();
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        assertThat(posted.get()).isEqualTo(1);
        assertThat(ledger.balance("alice", "GBP")).isZero();
    }

    @Test
    void propertyRandomPostingsAndReversalsAlwaysReconcile() {
        Random rnd = new Random(17);
        List<String> accounts = List.of("cash", "alice", "bob", "fees", "fxdesk");
        for (int i = 0; i < 2_000; i++) {
            String from = accounts.get(rnd.nextInt(5)), to = accounts.get(rnd.nextInt(5));
            if (from.equals(to)) continue;
            ledger.transfer("t" + i, from, to, rnd.nextBoolean() ? "GBP" : "EUR", BigDecimal.valueOf(rnd.nextInt(1, 100_000), 2));
            if (i > 0 && rnd.nextInt(10) == 0) {
                try {
                    ledger.reverse("r" + i, "t" + rnd.nextInt(i), "random correction");
                } catch (IllegalArgumentException skippedSelfTransferOrUnknown) {
                    // the chosen original may not exist (skipped self-transfer); that is fine for this property
                }
            }
        }
        assertThat(ledger.reconcile()).isEmpty();
        assertThat(ledger.systemTotal("GBP")).isZero();
        assertThat(ledger.systemTotal("EUR")).isZero();
    }
}
