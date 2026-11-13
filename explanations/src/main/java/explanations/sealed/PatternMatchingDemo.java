package explanations.sealed;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Every flavour of pattern matching over the sealed PaymentOutcome:
 * type patterns, record deconstruction, nested patterns, guards, `case null`, and the exhaustiveness guarantee.
 */
public final class PatternMatchingDemo {
    private PatternMatchingDemo() {
    }

    /** 1. Type patterns, exhaustive without default. */
    public static String describe(PaymentOutcome o) {
        return switch (o) {
            case Success s -> "settled " + s.settled() + " (" + s.authCode() + ")";
            case Failure f -> "failed: " + f.reason() + (f.retryable() ? ", retryable" : "");
            case Pending p -> "pending until " + p.checkAfter();
        };
    }

    /** 2. Record deconstruction patterns bind components directly. */
    public static BigDecimal settledAmount(PaymentOutcome o) {
        return switch (o) {
            case Success(String id, BigDecimal settled, String auth, Instant at) -> settled;
            case Failure f -> BigDecimal.ZERO;
            case Pending p -> BigDecimal.ZERO;
        };
    }

    /** 3. Guards refine a case; the unguarded case for the same type must follow or the switch is not exhaustive. */
    public static String nextAction(PaymentOutcome o, Instant now) {
        return switch (o) {
            case Success s -> "notify customer";
            case Failure f when f.retryable() -> "schedule retry";
            case Failure f when f.reason() == Failure.Reason.FRAUD -> "alert fraud team";
            case Failure f -> "notify decline";
            case Pending p when p.checkAfter().isAfter(now) -> "wait " + Duration.between(now, p.checkAfter()).toSeconds() + "s";
            case Pending p -> "poll provider now";
        };
    }

    /** 4. `case null` makes the switch null-safe instead of throwing NPE. */
    public static int httpStatus(PaymentOutcome o) {
        return switch (o) {
            case null -> 500;
            case Success s -> 200;
            case Pending p -> 202;
            case Failure f when f.reason() == Failure.Reason.INVALID_REQUEST -> 400;
            case Failure f when f.reason() == Failure.Reason.PROVIDER_ERROR -> 502;
            case Failure f -> 402;
        };
    }

    /** 5. instanceof patterns with flow scoping, for when a switch is overkill. */
    public static boolean isFinal(PaymentOutcome o) {
        if (o instanceof Pending) return false;
        if (o instanceof Failure f && f.retryable()) return false;
        return true;
    }

    /** 6. Nested patterns: a wrapper record containing the sealed type. */
    public record Envelope(String traceId, PaymentOutcome outcome) {
    }

    public static List<String> fraudTraceIds(List<Envelope> envelopes) {
        return envelopes.stream()
                .filter(e -> e instanceof Envelope(var trace, Failure(var id, var reason, var retryable)) && reason == Failure.Reason.FRAUD)
                .map(Envelope::traceId)
                .toList();
    }

    /** 7. Sealed + records are algebraic data types: fold them like a visitor without the visitor boilerplate. */
    public static BigDecimal totalSettled(List<PaymentOutcome> outcomes) {
        BigDecimal total = BigDecimal.ZERO;
        for (PaymentOutcome o : outcomes) {
            if (o instanceof Success(var id, var settled, var auth, var at)) total = total.add(settled);
        }
        return total;
    }

    /** Factory helpers so tests read well. */
    public static PaymentOutcome success(String id, String amount) {
        return new Success(id, new BigDecimal(amount), "A1", Instant.EPOCH);
    }

    public static PaymentOutcome failure(String id, Failure.Reason reason, boolean retryable) {
        return new Failure(id, reason, retryable);
    }

    public static PaymentOutcome pending(String id, Instant checkAfter) {
        return new Pending(id, checkAfter);
    }
}
