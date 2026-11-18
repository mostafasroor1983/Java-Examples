package explanations.sealed;

import explanations.sealed.OutcomeResponder.Action;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class OutcomeResponderTest {
    private static final Instant NOW = Instant.parse("2024-03-01T10:00:00Z");

    @Test
    void fraudDeclineIsIndistinguishableFromOrdinaryDeclineForTheClient() {
        var fraud = OutcomeResponder.decide(new Failure("p1", Failure.Reason.FRAUD, false), NOW);
        var funds = OutcomeResponder.decide(new Failure("p2", Failure.Reason.INSUFFICIENT_FUNDS, false), NOW);
        assertEquals(fraud.response(), funds.response(), "same status, code and message");
        assertFalse(fraud.response().message().toLowerCase().contains("fraud"));
        assertEquals(new Action.OpenFraudCase("p1"), fraud.action());
        assertEquals(new Action.None(), funds.action());
    }

    @Test
    void retryableProviderErrorIsReportedAsProcessingAndRetried() {
        var d = OutcomeResponder.decide(new Failure("p3", Failure.Reason.PROVIDER_ERROR, true), NOW);
        assertEquals(202, d.response().status());
        assertEquals(new Action.RetryWithBackoff("p3"), d.action());
        var nonRetryable = OutcomeResponder.decide(new Failure("p4", Failure.Reason.PROVIDER_ERROR, false), NOW);
        assertEquals(402, nonRetryable.response().status());
    }

    @Test
    void successPendingInvalidAndNull() {
        var ok = OutcomeResponder.decide(new Success("p5", new BigDecimal("1E+1"), "A1", NOW), NOW);
        assertEquals("Paid 10", ok.response().message(), "plain string, never scientific notation");
        var pending = OutcomeResponder.decide(new Pending("p6", NOW.plusSeconds(30)), NOW);
        assertEquals(new Action.ScheduleStatusCheck("p6", Duration.ofSeconds(30)), pending.action());
        var overdue = OutcomeResponder.decide(new Pending("p7", NOW.minusSeconds(5)), NOW);
        assertEquals(new Action.ScheduleStatusCheck("p7", Duration.ZERO), overdue.action(), "never a negative delay");
        assertEquals(400, OutcomeResponder.decide(new Failure("p8", Failure.Reason.INVALID_REQUEST, false), NOW).response().status());
        assertEquals(500, OutcomeResponder.decide(null, NOW).response().status());
    }
}
