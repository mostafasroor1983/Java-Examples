package explanations.sealed;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatternMatchingDemoTest {
    private final PaymentOutcome ok = PatternMatchingDemo.success("p1", "10.00");
    private final PaymentOutcome fraud = PatternMatchingDemo.failure("p2", Failure.Reason.FRAUD, false);
    private final PaymentOutcome provider = PatternMatchingDemo.failure("p3", Failure.Reason.PROVIDER_ERROR, true);
    private final PaymentOutcome invalid = PatternMatchingDemo.failure("p4", Failure.Reason.INVALID_REQUEST, false);
    private final PaymentOutcome pendingLater = PatternMatchingDemo.pending("p5", Instant.EPOCH.plusSeconds(30));
    private final PaymentOutcome pendingNow = PatternMatchingDemo.pending("p6", Instant.EPOCH);

    @Test
    void describeAndSettledAmount() {
        assertEquals("settled 10.00 (A1)", PatternMatchingDemo.describe(ok));
        assertEquals("failed: PROVIDER_ERROR, retryable", PatternMatchingDemo.describe(provider));
        assertTrue(PatternMatchingDemo.describe(pendingLater).startsWith("pending until"));
        assertEquals(new BigDecimal("10.00"), PatternMatchingDemo.settledAmount(ok));
        assertEquals(BigDecimal.ZERO, PatternMatchingDemo.settledAmount(fraud));
    }

    @Test
    void guardsAndNullCase() {
        Instant now = Instant.EPOCH.plusSeconds(10);
        assertEquals("notify customer", PatternMatchingDemo.nextAction(ok, now));
        assertEquals("schedule retry", PatternMatchingDemo.nextAction(provider, now));
        assertEquals("alert fraud team", PatternMatchingDemo.nextAction(fraud, now));
        assertEquals("notify decline", PatternMatchingDemo.nextAction(invalid, now));
        assertEquals("wait 20s", PatternMatchingDemo.nextAction(pendingLater, now));
        assertEquals("poll provider now", PatternMatchingDemo.nextAction(pendingNow, now));
        assertEquals(500, PatternMatchingDemo.httpStatus(null));
        assertEquals(200, PatternMatchingDemo.httpStatus(ok));
        assertEquals(202, PatternMatchingDemo.httpStatus(pendingNow));
        assertEquals(400, PatternMatchingDemo.httpStatus(invalid));
        assertEquals(502, PatternMatchingDemo.httpStatus(provider));
        assertEquals(402, PatternMatchingDemo.httpStatus(fraud));
    }

    @Test
    void instanceofNestedAndFold() {
        assertTrue(PatternMatchingDemo.isFinal(ok));
        assertTrue(PatternMatchingDemo.isFinal(fraud));
        assertFalse(PatternMatchingDemo.isFinal(provider));
        assertFalse(PatternMatchingDemo.isFinal(pendingNow));
        var envelopes = List.of(new PatternMatchingDemo.Envelope("t1", ok), new PatternMatchingDemo.Envelope("t2", fraud), new PatternMatchingDemo.Envelope("t3", provider));
        assertEquals(List.of("t2"), PatternMatchingDemo.fraudTraceIds(envelopes));
        assertEquals(new BigDecimal("15.50"), PatternMatchingDemo.totalSettled(List.of(ok, fraud, PatternMatchingDemo.success("p9", "5.50"))));
    }

    @Test
    void sealedContractAndValidation() {
        assertTrue(PaymentOutcome.class.isSealed());
        assertEquals(3, PaymentOutcome.class.getPermittedSubclasses().length);
        assertThrows(IllegalArgumentException.class, () -> PatternMatchingDemo.success("p", "0"));
        assertThrows(NullPointerException.class, () -> new Failure("p", null, false));
        assertThrows(NullPointerException.class, () -> new Pending("p", null));
        assertEquals(PatternMatchingDemo.success("p1", "10.00"), ok, "records give structural equality");
    }
}
