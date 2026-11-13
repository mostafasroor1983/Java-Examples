package module13.structured;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;

/**
 * Structured concurrency (preview in 21): subtasks are scoped to a block; if one fails the others are cancelled;
 * the scope cannot be exited until every subtask is done. No leaked threads, no orphaned work, readable stack traces.
 */
public final class StructuredPaymentCheck {
    public record Balance(String accountId, BigDecimal available) {
    }

    public record Fraud(int score) {
    }

    public record Limits(BigDecimal dailyRemaining) {
    }

    public record Decision(boolean approved, String reason) {
    }

    public interface Ports {
        Balance balance(String accountId) throws Exception;

        Fraud fraud(String accountId, BigDecimal amount) throws Exception;

        Limits limits(String accountId) throws Exception;
    }

    private final Ports ports;

    public StructuredPaymentCheck(Ports ports) {
        this.ports = ports;
    }

    /** ShutdownOnFailure: all three must succeed; the first failure cancels the rest and is rethrown. */
    public Decision check(String accountId, BigDecimal amount, Duration deadline) throws InterruptedException, ExecutionException, TimeoutException {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            StructuredTaskScope.Subtask<Balance> balance = scope.fork(() -> ports.balance(accountId));
            StructuredTaskScope.Subtask<Fraud> fraud = scope.fork(() -> ports.fraud(accountId, amount));
            StructuredTaskScope.Subtask<Limits> limits = scope.fork(() -> ports.limits(accountId));

            scope.joinUntil(Instant.now().plus(deadline));
            scope.throwIfFailed();

            if (fraud.get().score() > 80) return new Decision(false, "fraud score " + fraud.get().score());
            if (balance.get().available().compareTo(amount) < 0) return new Decision(false, "insufficient funds");
            if (limits.get().dailyRemaining().compareTo(amount) < 0) return new Decision(false, "daily limit");
            return new Decision(true, "ok");
        }
    }

    /** ShutdownOnSuccess: first FX venue to answer wins; the slower ones are cancelled. */
    public static BigDecimal fastestRate(List<Callable<BigDecimal>> venues, Duration deadline) throws InterruptedException, ExecutionException, TimeoutException {
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<BigDecimal>()) {
            for (Callable<BigDecimal> v : venues) scope.fork(v);
            scope.joinUntil(Instant.now().plus(deadline));
            return scope.result();
        }
    }

    /** Custom policy: collect every outcome, let the caller decide (partial results are fine for a dashboard). */
    public static List<String> collectAll(List<Callable<String>> tasks) throws InterruptedException {
        try (var scope = new StructuredTaskScope<String>()) {
            List<StructuredTaskScope.Subtask<String>> subtasks = tasks.stream().map(scope::fork).toList();
            scope.join();
            return subtasks.stream()
                    .map(s -> s.state() == StructuredTaskScope.Subtask.State.SUCCESS ? s.get() : "FAILED:" + s.exception().getMessage())
                    .toList();
        }
    }
}
