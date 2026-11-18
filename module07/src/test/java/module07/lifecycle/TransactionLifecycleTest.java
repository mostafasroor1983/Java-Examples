package module07.lifecycle;

import module07.lifecycle.TxCommand.*;
import module07.lifecycle.TxState.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class TransactionLifecycleTest {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    @Test
    void happyPathWithPartialCaptureAndRefunds() {
        TxState s = new Initiated("TX-1", HUNDRED);
        s = TransactionLifecycle.apply(s, new Authorise("A1B2"));
        s = TransactionLifecycle.apply(s, new Capture(new BigDecimal("80.00"))); // e.g. hotel final bill lower than hold
        s = TransactionLifecycle.apply(s, new Settle());
        s = TransactionLifecycle.apply(s, new Refund(new BigDecimal("30.00")));
        s = TransactionLifecycle.apply(s, new Refund(new BigDecimal("50.00")));
        assertEquals(new Settled("TX-1", new BigDecimal("80.00"), new BigDecimal("80.00")), s);
        TxState fullyRefunded = s;
        assertThrows(IllegalArgumentException.class,
                () -> TransactionLifecycle.apply(fullyRefunded, new Refund(new BigDecimal("0.01"))), "no over-refund");
    }

    @Test
    void businessLimitsAndIllegalTransitions() {
        TxState auth = new Authorised("TX-2", HUNDRED, "C");
        assertThrows(IllegalArgumentException.class, () -> TransactionLifecycle.apply(auth, new Capture(new BigDecimal("100.01"))));
        assertThrows(IllegalArgumentException.class, () -> TransactionLifecycle.apply(auth, new Capture(BigDecimal.ZERO)));
        assertEquals(new Reversed("TX-2"), TransactionLifecycle.apply(auth, new Reverse()));
        var ex = assertThrows(IllegalStateException.class,
                () -> TransactionLifecycle.apply(new Reversed("TX-2"), new Settle()));
        assertTrue(ex.getMessage().contains("Settle not allowed in Reversed"));
        assertThrows(IllegalStateException.class, () -> TransactionLifecycle.apply(new Initiated("T", HUNDRED), new Refund(HUNDRED)));
    }

    @Test
    void java17AndJava21ImplementationsAgreeOnEveryCombination() {
        List<TxState> states = List.of(new Initiated("T", HUNDRED), new Authorised("T", HUNDRED, "C"),
                new Captured("T", HUNDRED, new BigDecimal("60")), new Settled("T", new BigDecimal("60"), new BigDecimal("10")),
                new Reversed("T"), new Failed("T", "x"));
        List<TxCommand> commands = List.of(new Authorise("C"), new Capture(new BigDecimal("50")), new Capture(new BigDecimal("500")),
                new Settle(), new Refund(new BigDecimal("50")), new Refund(new BigDecimal("51")), new Reverse(), new Fail("timeout"));
        for (TxState s : states) {
            for (TxCommand c : commands) {
                assertEquals(outcome(TransactionLifecycle::apply, s, c), outcome(TransactionLifecycle::applyJava17, s, c), s + " + " + c);
            }
        }
    }

    private static Object outcome(BiFunction<TxState, TxCommand, TxState> f, TxState s, TxCommand c) {
        try {
            return f.apply(s, c);
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName() + ":" + e.getMessage();
        }
    }
}
