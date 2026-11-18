package module04.delay;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Retries failed outbound payment submissions (e.g. a scheme/PSP returned 503) with exponential backoff using a
 * {@link DelayQueue}: elements only become visible to {@code take()} once their delay has expired, so one worker
 * thread can serve thousands of pending retries without a sleeping thread per payment.
 *
 * <p>After {@code maxAttempts} the payment is moved to a dead-letter list for manual investigation: never retry
 * money movement forever, and never drop it silently.
 *
 * <p>Correctness caveat: retrying a payment submission is only safe if the downstream call is idempotent
 * (same idempotency key on every attempt). The key travels inside {@link RetryTask}.
 *
 * <p>In-memory caveat: pending retries are lost on restart. Production systems persist the retry state
 * (DB row with {@code next_attempt_at}, or a broker with delayed delivery) so a crash does not lose payments.
 */
public final class PaymentRetryScheduler implements AutoCloseable {

    public record RetryTask(String paymentId, String idempotencyKey, int attempt, long dueAtNanos) implements Delayed {
        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(dueAtNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            // must be consistent with getDelay; ties broken by id so ordering is deterministic
            int c = Long.compare(dueAtNanos, ((RetryTask) other).dueAtNanos);
            return c != 0 ? c : paymentId.compareTo(((RetryTask) other).paymentId);
        }
    }

    private final DelayQueue<RetryTask> queue = new DelayQueue<>();
    private final List<RetryTask> deadLetters = new CopyOnWriteArrayList<>();
    private final Predicate<RetryTask> submitter; // returns true when the downstream accepted the payment
    private final Duration baseDelay;
    private final Duration maxDelay;
    private final int maxAttempts;
    private final Thread worker;
    private volatile boolean running = true;

    public PaymentRetryScheduler(Predicate<RetryTask> submitter, Duration baseDelay, Duration maxDelay, int maxAttempts) {
        this.submitter = Objects.requireNonNull(submitter, "submitter");
        this.baseDelay = Objects.requireNonNull(baseDelay, "baseDelay");
        this.maxDelay = Objects.requireNonNull(maxDelay, "maxDelay");
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        this.maxAttempts = maxAttempts;
        this.worker = Thread.ofPlatform().name("payment-retry-worker").daemon(true).start(this::loop);
    }

    /** Schedules attempt #1 after the base delay. */
    public void scheduleRetry(String paymentId, String idempotencyKey) {
        if (!running) throw new IllegalStateException("scheduler closed");
        queue.put(new RetryTask(paymentId, idempotencyKey, 1, System.nanoTime() + backoff(1).toNanos()));
    }

    /** Exponential backoff capped at maxDelay: base, 2*base, 4*base, ... (add jitter in production, see module12). */
    Duration backoff(int attempt) {
        long factor = 1L << Math.min(attempt - 1, 30);
        Duration d = baseDelay.multipliedBy(factor);
        return d.compareTo(maxDelay) > 0 ? maxDelay : d;
    }

    private void loop() {
        while (running) {
            RetryTask task;
            try {
                task = queue.take(); // blocks until the head's delay expires
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            boolean accepted;
            try {
                accepted = submitter.test(task);
            } catch (RuntimeException e) {
                accepted = false; // a throwing submitter must not kill the worker thread
            }
            if (accepted) continue;
            if (task.attempt() >= maxAttempts) {
                deadLetters.add(task);
            } else {
                int next = task.attempt() + 1;
                queue.put(new RetryTask(task.paymentId(), task.idempotencyKey(), next,
                        System.nanoTime() + backoff(next).toNanos()));
            }
        }
    }

    public List<RetryTask> deadLetters() {
        return List.copyOf(deadLetters);
    }

    public int pending() {
        return queue.size();
    }

    /** Stops the worker. Pending tasks are returned so the caller can persist them; nothing is silently lost. */
    public List<RetryTask> shutdown() {
        running = false;
        worker.interrupt();
        List<RetryTask> drained = new java.util.ArrayList<>();
        // drainTo only moves EXPIRED elements from a DelayQueue; iterate + remove to take everything
        for (RetryTask t : queue) if (queue.remove(t)) drained.add(t);
        return drained;
    }

    @Override
    public void close() {
        shutdown();
    }
}
