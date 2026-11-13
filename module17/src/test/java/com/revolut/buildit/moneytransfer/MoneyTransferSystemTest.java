package com.revolut.buildit.moneytransfer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class MoneyTransferSystemTest {
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2024-03-01T10:00:00Z"));
    private MoneyTransferSystem sys;

    @BeforeEach
    void setUp() {
        sys = new MoneyTransferSystem(new BigDecimal("500"), now::get);
        sys.openAccount("A", new BigDecimal("1000"));
        sys.openAccount("B", new BigDecimal("1000"));
        sys.openAccount("C", new BigDecimal("1000"));
    }

    @Test
    void outcomesAndHistory() {
        assertThat(sys.transfer("t1", "A", "B", new BigDecimal("300")).outcome()).isEqualTo(MoneyTransferSystem.Outcome.COMPLETED);
        assertThat(sys.transfer("t1", "A", "B", new BigDecimal("300")).outcome()).as("idempotent").isEqualTo(MoneyTransferSystem.Outcome.DUPLICATE);
        assertThat(sys.transfer("t2", "A", "B", new BigDecimal("300")).outcome()).isEqualTo(MoneyTransferSystem.Outcome.DAILY_LIMIT_EXCEEDED);
        assertThat(sys.transfer("t3", "A", "B", new BigDecimal("2000")).outcome()).isEqualTo(MoneyTransferSystem.Outcome.INSUFFICIENT_FUNDS);
        assertThat(sys.transfer("t4", "A", "A", BigDecimal.ONE).outcome()).isEqualTo(MoneyTransferSystem.Outcome.SAME_ACCOUNT);
        assertThat(sys.transfer("t5", "A", "Z", BigDecimal.ONE).outcome()).isEqualTo(MoneyTransferSystem.Outcome.UNKNOWN_ACCOUNT);
        assertThat(sys.transfer("t6", "A", "B", BigDecimal.ZERO).outcome()).isEqualTo(MoneyTransferSystem.Outcome.INVALID_AMOUNT);
        assertThat(sys.balance("A")).isEqualTo(new BigDecimal("700"));
        assertThat(sys.balance("B")).isEqualTo(new BigDecimal("1300"));
        assertThat(sys.history("A")).extracting(MoneyTransferSystem.Transfer::outcome)
                .containsExactly(MoneyTransferSystem.Outcome.COMPLETED, MoneyTransferSystem.Outcome.DAILY_LIMIT_EXCEEDED, MoneyTransferSystem.Outcome.INSUFFICIENT_FUNDS);
        assertThat(sys.history("B")).hasSize(1);
        assertThat(sys.find("t1")).isPresent();
        assertThat(sys.find("nope")).isEmpty();

        now.set(Instant.parse("2024-03-02T00:00:01Z")); // next day: limit resets
        assertThat(sys.transfer("t7", "A", "B", new BigDecimal("300")).outcome()).isEqualTo(MoneyTransferSystem.Outcome.COMPLETED);
    }

    @Test
    void concurrentOpposingTransfersConserveMoney() throws Exception {
        MoneyTransferSystem big = new MoneyTransferSystem(new BigDecimal("1000000"), now::get);
        for (String id : new String[]{"A", "B", "C"}) big.openAccount(id, new BigDecimal("1000"));
        String[][] pairs = {{"A", "B"}, {"B", "A"}, {"B", "C"}, {"C", "B"}, {"C", "A"}, {"A", "C"}};
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(pairs.length);
        for (String[] p : pairs) new Thread(() -> { try { start.await(); for (int i = 0; i < 1000; i++) big.transfer(p[0] + p[1] + i, p[0], p[1], BigDecimal.ONE); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } finally { done.countDown(); } }).start();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(big.totalMoney()).isEqualTo(new BigDecimal("3000"));
    }
}
