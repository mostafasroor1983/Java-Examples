package module08.circuitbreaker;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Circuit breaker: CLOSED (normal) -> OPEN after N consecutive failures (fail fast, no calls) ->
 * HALF_OPEN after a cooldown (one trial call) -> CLOSED on success / OPEN on failure.
 * Protects a payments service from hammering a dead provider and lets it recover.
 */
public final class CircuitBreaker {
    public enum State { CLOSED, OPEN, HALF_OPEN }

    public static final class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String name) {
            super("circuit '" + name + "' is open");
        }
    }

    private final String name;
    private final int failureThreshold;
    private final Duration cooldown;
    private final Supplier<Instant> clock;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile Instant openedAt;
    private final AtomicInteger trialInFlight = new AtomicInteger();

    public CircuitBreaker(String name, int failureThreshold, Duration cooldown, Supplier<Instant> clock) {
        if (failureThreshold <= 0) throw new IllegalArgumentException("failureThreshold must be positive");
        if (cooldown.isNegative() || cooldown.isZero()) throw new IllegalArgumentException("cooldown must be positive");
        this.name = Objects.requireNonNull(name);
        this.failureThreshold = failureThreshold;
        this.cooldown = cooldown;
        this.clock = Objects.requireNonNull(clock);
    }

    public <T> T call(Supplier<T> action) {
        State s = currentState();
        if (s == State.OPEN) {
            throw new CircuitOpenException(name);
        }
        if (s == State.HALF_OPEN && !trialInFlight.compareAndSet(0, 1)) {
            throw new CircuitOpenException(name); // only one trial call at a time
        }
        try {
            T result = action.get();
            onSuccess();
            return result;
        } catch (RuntimeException e) {
            onFailure();
            throw e;
        } finally {
            if (s == State.HALF_OPEN) trialInFlight.set(0);
        }
    }

    public State currentState() {
        State s = state.get();
        if (s == State.OPEN && !clock.get().isBefore(openedAt.plus(cooldown))) {
            if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) return State.HALF_OPEN;
            return state.get();
        }
        return s;
    }

    public int consecutiveFailures() {
        return consecutiveFailures.get();
    }

    private synchronized void onSuccess() {
        consecutiveFailures.set(0);
        state.set(State.CLOSED);
    }

    private synchronized void onFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (state.get() == State.HALF_OPEN || failures >= failureThreshold) {
            openedAt = clock.get();
            state.set(State.OPEN);
        }
    }
}
