package explanations.sealed;

import java.time.Duration;
import java.time.Instant;

/**
 * Turns a {@link PaymentOutcome} into (a) the HTTP response the client sees and (b) the internal follow-up action.
 * One exhaustive switch with record patterns and guards (Java 21 final; preview in 19/20) replaces scattered if/else.
 *
 * <p>Security rule encoded in the type-driven switch: a FRAUD decline must look EXACTLY like a generic decline to the
 * client (same status, same code, same message). Telling a fraudster "blocked for fraud" helps them tune their next
 * attempt; internally, a case is opened for analysts. INVALID_REQUEST is the only failure whose detail is safe to reveal.
 */
public final class OutcomeResponder {
    private OutcomeResponder() {
    }

    public record ApiResponse(int status, String code, String message) {
    }

    public sealed interface Action {
        record None() implements Action { }

        record ScheduleStatusCheck(String paymentId, Duration after) implements Action { }

        record RetryWithBackoff(String paymentId) implements Action { }

        record OpenFraudCase(String paymentId) implements Action { }
    }

    public record Decision(ApiResponse response, Action action) {
    }

    private static final ApiResponse GENERIC_DECLINE = new ApiResponse(402, "PAYMENT_DECLINED", "The payment was declined.");

    public static Decision decide(PaymentOutcome outcome, Instant now) {
        return switch (outcome) {
            case null -> new Decision(new ApiResponse(500, "INTERNAL_ERROR", "Unexpected error."), new Action.None());
            case Success(var id, var settled, var auth, var at) ->
                    new Decision(new ApiResponse(200, "SETTLED", "Paid " + settled.toPlainString()), new Action.None());
            case Pending(var id, var checkAfter) -> new Decision(
                    new ApiResponse(202, "PROCESSING", "The payment is being processed."),
                    new Action.ScheduleStatusCheck(id, checkAfter.isAfter(now) ? Duration.between(now, checkAfter) : Duration.ZERO));
            case Failure(var id, var reason, var retryable) when reason == Failure.Reason.FRAUD ->
                    new Decision(GENERIC_DECLINE, new Action.OpenFraudCase(id)); // indistinguishable for the client
            case Failure(var id, var reason, var retryable) when reason == Failure.Reason.INVALID_REQUEST ->
                    new Decision(new ApiResponse(400, "INVALID_REQUEST", "The request is invalid."), new Action.None());
            case Failure(var id, var reason, var retryable) when retryable ->
                    new Decision(new ApiResponse(202, "PROCESSING", "The payment is being processed."), new Action.RetryWithBackoff(id));
            case Failure f -> new Decision(GENERIC_DECLINE, new Action.None());
        };
    }
}
