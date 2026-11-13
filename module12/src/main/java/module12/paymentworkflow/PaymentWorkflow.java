package module12.paymentworkflow;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * End-to-end async payment: validate -> fraud check (timeout + fallback to manual review) -> debit ->
 * notify (fire-and-forget, failures logged not propagated). Includes retry with bounded attempts.
 */
public final class PaymentWorkflow {
    public enum Status { APPROVED, MANUAL_REVIEW, DECLINED }

    public record Payment(String id, String accountId, BigDecimal amount) {
        public Payment {
            Objects.requireNonNull(id);
            Objects.requireNonNull(accountId);
            Objects.requireNonNull(amount);
            if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        }
    }

    public record Outcome(String paymentId, Status status, String detail) {
    }

    public interface Ports {
        CompletableFuture<Integer> fraudScore(Payment p);          // 0..100

        CompletableFuture<Boolean> debit(Payment p);               // false = insufficient funds

        CompletableFuture<Void> notify(Payment p, Status s);
    }

    private final Ports ports;
    private final Executor executor;
    private final Duration fraudTimeout;
    private final List<String> log;

    public PaymentWorkflow(Ports ports, Executor executor, Duration fraudTimeout, List<String> log) {
        this.ports = Objects.requireNonNull(ports);
        this.executor = Objects.requireNonNull(executor);
        this.fraudTimeout = Objects.requireNonNull(fraudTimeout);
        this.log = Objects.requireNonNull(log);
    }

    public CompletableFuture<Outcome> process(Payment p) {
        return fraudWithFallback(p)
                .thenComposeAsync(score -> {
                    if (score < 0) return CompletableFuture.completedFuture(new Outcome(p.id(), Status.MANUAL_REVIEW, "fraud service unavailable"));
                    if (score > 80) return CompletableFuture.completedFuture(new Outcome(p.id(), Status.DECLINED, "fraud score " + score));
                    return retry(() -> ports.debit(p), 3)
                            .thenApply(ok -> ok ? new Outcome(p.id(), Status.APPROVED, "debited " + p.amount())
                                    : new Outcome(p.id(), Status.DECLINED, "insufficient funds"));
                }, executor)
                .thenApply(outcome -> {
                    // fire-and-forget: notification failures must not fail the payment
                    ports.notify(p, outcome.status()).whenComplete((v, ex) -> {
                        if (ex != null) synchronized (log) { log.add("notify failed for " + p.id() + ": " + root(ex).getMessage()); }
                    });
                    return outcome;
                });
    }

    /** -1 means "unknown" so the caller can route to manual review instead of blindly approving or declining. */
    private CompletableFuture<Integer> fraudWithFallback(Payment p) {
        return ports.fraudScore(p)
                .orTimeout(fraudTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    synchronized (log) { log.add("fraud fallback for " + p.id() + ": " + root(ex).getClass().getSimpleName()); }
                    return -1;
                });
    }

    /** Async retry without blocking: recurse through exceptionallyCompose. */
    static <T> CompletableFuture<T> retry(Supplier<CompletableFuture<T>> action, int attempts) {
        AtomicInteger remaining = new AtomicInteger(attempts);
        return attempt(action, remaining);
    }

    private static <T> CompletableFuture<T> attempt(Supplier<CompletableFuture<T>> action, AtomicInteger remaining) {
        return action.get().exceptionallyCompose(ex -> {
            if (remaining.decrementAndGet() <= 0) return CompletableFuture.failedFuture(root(ex));
            return attempt(action, remaining);
        });
    }

    private static Throwable root(Throwable t) {
        while (t.getCause() != null && t instanceof java.util.concurrent.CompletionException) t = t.getCause();
        return t;
    }
}
