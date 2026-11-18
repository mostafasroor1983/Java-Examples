package module18;

import module18.ch10.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLTransientConnectionException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Chapter 10 - Exceptions (items 69-77). */
class Chapter10Test {

    @Test
    void item69_stateTestingInsteadOfExceptions() {
        assertEquals(new BigDecimal("3"), Item69ExceptionsForExceptional.sumBad(List.of(BigDecimal.ONE, new BigDecimal("2"))));
        var w = new Item69ExceptionsForExceptional.Wallet(BigDecimal.TEN);
        assertFalse(w.canDebit(new BigDecimal("11")));
        assertTrue(w.tryDebit(new BigDecimal("11")).isEmpty());
        assertEquals(new BigDecimal("4"), w.tryDebit(new BigDecimal("6")).orElseThrow());
    }

    @Test
    void item70_checkedCarriesRecoveryInfo() throws Exception {
        assertEquals("AUTH-OK", Item70CheckedVsRuntime.authorise(BigDecimal.TEN, "00"));
        var ex = assertThrows(Item70CheckedVsRuntime.CardDeclinedException.class, () -> Item70CheckedVsRuntime.authorise(BigDecimal.TEN, "91"));
        assertTrue(ex.retryable());
        assertEquals("ISSUER_UNAVAILABLE", ex.declineCode());
        assertThrows(IllegalArgumentException.class, () -> Item70CheckedVsRuntime.authorise(BigDecimal.ZERO, "00"));
    }

    @Test
    void item71_optionalComposesWithStreams() {
        assertThrows(Item71UnnecessaryChecked.AccountNotFoundException.class, () -> Item71UnnecessaryChecked.findOrThrow("X"));
        assertEquals(List.of("ACC-1"), Item71UnnecessaryChecked.existing(List.of("ACC-1", "X")));
    }

    @Test
    void item72_standardExceptions() {
        assertThrows(IllegalArgumentException.class, () -> new Item72StandardExceptions.CardPayment(BigDecimal.ZERO));
        var p = new Item72StandardExceptions.CardPayment(BigDecimal.TEN);
        p.cancel();
        assertThrows(IllegalStateException.class, p::capture);
        assertThrows(UnsupportedOperationException.class, () -> new Item72StandardExceptions.CashPayment().refund());
        assertThrows(IndexOutOfBoundsException.class, () -> Item72StandardExceptions.installment(List.of("m1"), 3));
    }

    @Test
    void item73_translatedWithCauseAndClassification() {
        assertEquals(12_345, Item73ExceptionTranslation.balance("A", false, false));
        var transient_ = assertThrows(Item73ExceptionTranslation.LedgerUnavailableException.class,
                () -> Item73ExceptionTranslation.balance("A", true, false));
        assertTrue(transient_.retryable());
        assertInstanceOf(SQLTransientConnectionException.class, transient_.getCause());
        assertFalse(assertThrows(Item73ExceptionTranslation.LedgerUnavailableException.class,
                () -> Item73ExceptionTranslation.balance("A", false, true)).retryable());
    }

    @Test
    void item74_documentedExceptionsBehaveAsDocumented() throws Exception {
        assertEquals(new BigDecimal("100"), Item74DocumentExceptions.authoriseAtm(new BigDecimal("200"), new BigDecimal("200"), new BigDecimal("500")));
        assertThrows(Item74DocumentExceptions.DailyLimitExceededException.class,
                () -> Item74DocumentExceptions.authoriseAtm(new BigDecimal("400"), new BigDecimal("200"), new BigDecimal("500")));
        assertThrows(IllegalArgumentException.class,
                () -> Item74DocumentExceptions.authoriseAtm(new BigDecimal("15"), BigDecimal.ZERO, new BigDecimal("500")));
    }

    @Test
    void item75_failureCaptureWithoutSecrets() {
        var e = new Item75FailureCaptureInfo.LimitExceededException("ACC-7", new BigDecimal("1E+3"), new BigDecimal("250.00"));
        assertEquals("limit exceeded: account=ACC-7, requested=1000, remainingLimit=250.00", e.getMessage());
        assertEquals(new BigDecimal("250.00"), e.remainingLimit());
    }

    @Test
    void item76_failureAtomicity() {
        var broken = new Item76FailureAtomicity.BrokenBatch();
        assertThrows(IllegalArgumentException.class, () -> broken.postAll(List.of(BigDecimal.ONE, BigDecimal.ZERO)));
        assertEquals(1, broken.size(), "half a batch applied");
        var atomic = new Item76FailureAtomicity.AtomicBatch();
        assertThrows(IllegalArgumentException.class, () -> atomic.postAll(List.of(BigDecimal.ONE, BigDecimal.ZERO)));
        assertEquals(0, atomic.size());
    }

    @Test
    void item77_swallowedVsHandled() {
        Runnable failing = () -> { throw new IllegalStateException("SMS gateway down"); };
        assertTrue(Item77DontIgnoreExceptions.notifyBad(failing), "lies: reports success");
        assertFalse(Item77DontIgnoreExceptions.notifyCustomer(failing));
        int before = Item77DontIgnoreExceptions.SWALLOWED_CLOSE_FAILURES.get();
        Item77DontIgnoreExceptions.closeQuietly(() -> { throw new java.io.IOException("x"); });
        assertEquals(before + 1, Item77DontIgnoreExceptions.SWALLOWED_CLOSE_FAILURES.get());
    }
}
