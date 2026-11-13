package module08.wrapping;

import module08.hierarchy.PaymentProviderException;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionWrappingTest {

    @Test
    void swallowingHidesFailures() {
        assertNull(ExceptionWrapping.authoriseBad(id -> { throw new SocketTimeoutException(); }, "p1"));
    }

    @Test
    void losingCauseIsDetectable() {
        PaymentProviderException e = assertThrows(PaymentProviderException.class,
                () -> ExceptionWrapping.authoriseLosesCause(id -> { throw new SocketTimeoutException("slow"); }, "p1"));
        assertNull(e.getCause());
        assertNull(ExceptionWrapping.findCause(e, SocketTimeoutException.class));
    }

    @Test
    void goodWrappingKeepsCauseAndClassifiesRetryability() {
        PaymentProviderException timeout = assertThrows(PaymentProviderException.class,
                () -> ExceptionWrapping.authorise(id -> { throw new SocketTimeoutException("slow"); }, "p1"));
        assertTrue(timeout.retryable());
        assertNotNull(ExceptionWrapping.findCause(timeout, SocketTimeoutException.class));

        PaymentProviderException other = assertThrows(PaymentProviderException.class,
                () -> ExceptionWrapping.authorise(id -> { throw new java.io.IOException("refused"); }, "p1"));
        assertFalse(other.retryable());

        IllegalStateException passthrough = assertThrows(IllegalStateException.class,
                () -> ExceptionWrapping.authorise(id -> { throw new IllegalStateException("bug"); }, "p1"));
        assertEquals("bug", passthrough.getMessage());
        assertEquals("OK", ExceptionWrapping.authorise(id -> "OK", "p1"));
    }

    @Test
    void interruptionIsRestored() {
        Thread.interrupted(); // clear
        assertThrows(PaymentProviderException.class,
                () -> ExceptionWrapping.authorise(id -> { throw new InterruptedException(); }, "p1"));
        assertTrue(Thread.interrupted(), "interrupt flag must be restored (and this call clears it again)");
    }

    @Test
    void suppressedExceptionsPreservePrimaryFailure() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ExceptionWrapping.failWithCleanup(
                () -> { throw new IllegalStateException("primary"); },
                () -> { throw new IllegalArgumentException("cleanup"); }));
        assertEquals("primary", e.getMessage());
        assertEquals(1, e.getSuppressed().length);
        assertEquals("cleanup", e.getSuppressed()[0].getMessage());
        assertThrows(IllegalArgumentException.class, () -> ExceptionWrapping.failWithCleanup(() -> { }, () -> { throw new IllegalArgumentException(); }));
    }
}
