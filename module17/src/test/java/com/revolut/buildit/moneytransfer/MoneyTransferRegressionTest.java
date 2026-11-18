package com.revolut.buildit.moneytransfer;

import com.revolut.buildit.moneytransfer.MoneyTransferSystem.Outcome;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/** Regression tests for bugs found in review. */
class MoneyTransferRegressionTest {

    @Test
    void lateArrivingTimestampFromYesterdayMustNotResetTodaysLimit() {
        // Clock order simulates two threads around midnight: the "yesterday" thread read the clock first but
        // acquired the account lock after a "today" transfer. It must not wipe today's spent amount.
        Instant today = Instant.parse("2024-03-02T00:00:01Z"), yesterday = Instant.parse("2024-03-01T23:59:59Z");
        Deque<Instant> ticks = new ArrayDeque<>(List.of(today, yesterday, today));
        var sys = new MoneyTransferSystem(new BigDecimal("100"), ticks::poll);
        sys.openAccount("A", new BigDecimal("1000"));
        sys.openAccount("B", BigDecimal.ZERO);

        assertThat(sys.transfer("t1", "A", "B", new BigDecimal("100")).outcome()).isEqualTo(Outcome.COMPLETED);
        sys.transfer("t2", "A", "B", new BigDecimal("1"));     // stale timestamp, counted against the current day
        assertThat(sys.transfer("t3", "A", "B", new BigDecimal("100")).outcome())
                .as("today's limit (100) is already used").isEqualTo(Outcome.DAILY_LIMIT_EXCEEDED);
    }

    @Test
    void unexpectedFailureReleasesTheTransferIdForRetry() {
        var sys = new MoneyTransferSystem(new BigDecimal("100"), () -> Instant.EPOCH);
        sys.openAccount("A", BigDecimal.TEN);
        sys.openAccount("B", BigDecimal.ZERO);
        assertThatThrownBy(() -> sys.transfer("t1", null, "B", BigDecimal.ONE)).isInstanceOf(NullPointerException.class);
        assertThat(sys.transfer("t1", "A", "B", BigDecimal.ONE).outcome()).as("id not burned").isEqualTo(Outcome.COMPLETED);
    }
}
