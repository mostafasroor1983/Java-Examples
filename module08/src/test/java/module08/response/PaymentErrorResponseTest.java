package module08.response;

import module08.circuitbreaker.CircuitBreaker;
import module08.hierarchy.InsufficientFundsException;
import module08.hierarchy.InvalidPaymentException;
import module08.hierarchy.PaymentProviderException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentErrorResponseTest {

    @Test
    void mapsDomainExceptionsToStableContracts() {
        var nsf = PaymentErrorResponse.from(new InsufficientFundsException("A", new BigDecimal("5.00"), new BigDecimal("9.00")));
        assertEquals(402, nsf.status());
        assertEquals("INSUFFICIENT_FUNDS", nsf.code());
        assertEquals("5.00", nsf.details().get("available"));
        assertFalse(nsf.retryable());

        assertEquals(400, PaymentErrorResponse.from(new InvalidPaymentException("amount must be positive")).status());
        assertEquals(503, PaymentErrorResponse.from(new PaymentProviderException("t", null, true)).status());
        assertEquals(502, PaymentErrorResponse.from(new PaymentProviderException("t", null, false)).status());
        assertEquals("CIRCUIT_OPEN", PaymentErrorResponse.from(new CircuitBreaker.CircuitOpenException("p")).code());
        assertEquals(400, PaymentErrorResponse.from(new IllegalArgumentException("x")).status());
    }

    @Test
    void unknownExceptionsNeverLeakDetails() {
        var r = PaymentErrorResponse.from(new RuntimeException("SELECT * FROM secrets failed"));
        assertEquals(500, r.status());
        assertEquals("Unexpected error", r.message());
        assertFalse(r.message().contains("SELECT"));
        assertThrows(UnsupportedOperationException.class, () -> r.details().put("x", "y"));
    }
}
