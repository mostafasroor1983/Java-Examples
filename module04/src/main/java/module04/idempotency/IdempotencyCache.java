package module04.idempotency;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Idempotency keys for payment APIs: the first caller with a key executes the operation,
 * concurrent callers with the same key get the same result (or wait for it), later callers get the cached response.
 *
 * computeIfAbsent runs the supplier at most once per key - this is the atomic "claim" step.
 * Entries carry a TTL and are evicted lazily on access and eagerly via purgeExpired().
 */
public final class IdempotencyCache<R> {
    private record Entry<R>(R response, Instant expiresAt) {
    }

    private final ConcurrentHashMap<String, Entry<R>> cache = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Supplier<Instant> clock;

    public IdempotencyCache(Duration ttl, Supplier<Instant> clock) {
        if (ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("ttl must be positive");
        this.ttl = ttl;
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Executes `operation` at most once per key within the TTL window.
     * Note: the supplier must not touch this cache (CHM forbids recursive updates).
     */
    public R execute(String idempotencyKey, Supplier<R> operation) {
        validateKey(idempotencyKey);
        Instant now = clock.get();
        Entry<R> entry = cache.compute(idempotencyKey, (k, existing) -> {
            if (existing != null && existing.expiresAt().isAfter(now)) {
                return existing;
            }
            return new Entry<>(operation.get(), now.plus(ttl));
        });
        return entry.response();
    }

    public Optional<R> peek(String idempotencyKey) {
        Entry<R> e = cache.get(idempotencyKey);
        if (e == null) return Optional.empty();
        if (!e.expiresAt().isAfter(clock.get())) {
            cache.remove(idempotencyKey, e);
            return Optional.empty();
        }
        return Optional.of(e.response());
    }

    public int purgeExpired() {
        Instant now = clock.get();
        int before = cache.size();
        cache.entrySet().removeIf(en -> !en.getValue().expiresAt().isAfter(now));
        return before - cache.size();
    }

    public int size() {
        return cache.size();
    }

    private static void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new IllegalArgumentException("idempotency key must be 1..128 non-blank chars");
        }
    }
}
