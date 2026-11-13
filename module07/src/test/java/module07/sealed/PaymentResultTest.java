package module07.sealed;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PaymentResultTest {
    private final PaymentResult success = new PaymentResult.Success("p1", "A1", new BigDecimal("10.00"), Instant.EPOCH);
    private final PaymentResult declinedNsf = new PaymentResult.Declined("p2", PaymentResult.DeclineReason.INSUFFICIENT_FUNDS);
    private final PaymentResult declinedFraud = new PaymentResult.Declined("p3", PaymentResult.DeclineReason.FRAUD_SUSPECTED);
    private final PaymentResult pending = new PaymentResult.Pending("p4", Instant.EPOCH.plusSeconds(60));
    private final PaymentResult failedRetry = new PaymentResult.Failed("p5", "timeout", true);
    private final PaymentResult failedFinal = new PaymentResult.Failed("p6", "bad request", false);

    @Test
    void exhaustiveMessagesAndStatuses() {
        assertEquals("Paid 10.00 (auth A1)", ResultHandler.customerMessage(success));
        assertEquals("Top up your account and try again", ResultHandler.customerMessage(declinedNsf));
        assertEquals("Payment declined: FRAUD_SUSPECTED", ResultHandler.customerMessage(declinedFraud));
        assertTrue(ResultHandler.customerMessage(pending).startsWith("Processing"));
        assertEquals("Temporary problem, retrying", ResultHandler.customerMessage(failedRetry));
        assertEquals("Payment failed: bad request", ResultHandler.customerMessage(failedFinal));
        assertEquals(200, ResultHandler.httpStatus(success));
        assertEquals(202, ResultHandler.httpStatus(pending));
        assertEquals(402, ResultHandler.httpStatus(declinedNsf));
        assertEquals(503, ResultHandler.httpStatus(failedRetry));
        assertEquals(500, ResultHandler.httpStatus(failedFinal));
    }

    @Test
    void finalityAndDeconstruction() {
        assertTrue(success.isFinal());
        assertTrue(declinedNsf.isFinal());
        assertFalse(pending.isFinal());
        assertFalse(failedRetry.isFinal());
        assertTrue(failedFinal.isFinal());
        assertEquals(new BigDecimal("10.00"), ResultHandler.settledOrZero(success));
        assertEquals(BigDecimal.ZERO, ResultHandler.settledOrZero(pending));
    }

    @Test
    void sealedPermitsAreClosed() {
        assertTrue(PaymentResult.class.isSealed());
        assertEquals(4, PaymentResult.class.getPermittedSubclasses().length);
        assertThrows(NullPointerException.class, () -> new PaymentResult.Success("p", null, BigDecimal.ONE, Instant.EPOCH));
    }
}
