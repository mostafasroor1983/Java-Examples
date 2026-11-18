package module04.cache;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Read-through FX rate cache ("memoizer") on ConcurrentHashMap.
 *
 * <p>Problems it solves, and how:
 * <ul>
 *   <li><b>Cache stampede</b>: 500 concurrent quote requests for EUR/USD on a cold cache must trigger ONE call to the
 *   rate provider. The map stores {@code CompletableFuture}s; {@code compute} installs a future atomically, and
 *   every other caller joins the same future.</li>
 *   <li><b>Slow loader inside a map lock</b>: the provider call runs OUTSIDE {@code compute} (we only create the
 *   future inside it), so other keys in the same bin are not blocked by network latency.</li>
 *   <li><b>Caching failures</b>: a failed load is removed (only if it is still the same future) so the next caller
 *   retries instead of seeing a cached exception for the whole TTL.</li>
 *   <li><b>Staleness</b>: each entry has a TTL; {@link #invalidate} supports push-based invalidation when the
 *   treasury desk publishes new rates.</li>
 * </ul>
 * FX quotes shown to a customer must also carry their own expiry (quote id + valid-until) so the executed rate
 * matches the quoted one even if the cache refreshes in between.
 */
public final class FxRateCache {
    private record Timed(CompletableFuture<BigDecimal> rate, Instant loadedAt) {
    }

    private final ConcurrentHashMap<String, Timed> cache = new ConcurrentHashMap<>();
    private final Function<String, BigDecimal> provider;
    private final Duration ttl;
    private final Supplier<Instant> clock;

    public FxRateCache(Function<String, BigDecimal> provider, Duration ttl, Supplier<Instant> clock) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public BigDecimal rate(String pair) {
        Instant now = clock.get();
        CompletableFuture<BigDecimal> mine = new CompletableFuture<>();
        Timed entry = cache.compute(pair, (k, existing) ->
                existing != null && now.isBefore(existing.loadedAt().plus(ttl)) && !existing.rate().isCompletedExceptionally()
                        ? existing
                        : new Timed(mine, now));
        if (entry.rate() == mine) { // we won: load outside the map's bin lock
            try {
                mine.complete(provider.apply(pair));
            } catch (RuntimeException e) {
                mine.completeExceptionally(e);
                cache.remove(pair, entry); // don't cache failures; only remove OUR entry
            }
        }
        try {
            return entry.rate().join();
        } catch (CompletionException e) {
            throw e.getCause() instanceof RuntimeException re ? re : e;
        }
    }

    public void invalidate(String pair) {
        cache.remove(pair);
    }

    public int size() {
        return cache.size();
    }
}
