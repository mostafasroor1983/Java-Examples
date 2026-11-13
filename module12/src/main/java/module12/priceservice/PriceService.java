package module12.priceservice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Best-price aggregation: query N FX venues in parallel, tolerate individual failures/timeouts,
 * return the best (lowest) quote with the venues that answered.
 */
public final class PriceService {
    public record Quote(String venue, BigDecimal rate) {
    }

    public record BestPrice(Quote best, List<String> respondedVenues, List<String> failedVenues) {
    }

    private final Map<String, Function<String, CompletableFuture<BigDecimal>>> venues; // venue -> pair -> rate
    private final Executor executor;
    private final Duration perVenueTimeout;

    public PriceService(Map<String, Function<String, CompletableFuture<BigDecimal>>> venues, Executor executor, Duration perVenueTimeout) {
        this.venues = Map.copyOf(venues);
        this.executor = Objects.requireNonNull(executor);
        this.perVenueTimeout = Objects.requireNonNull(perVenueTimeout);
    }

    public CompletableFuture<BestPrice> bestPrice(String pair) {
        List<String> names = List.copyOf(venues.keySet());
        List<CompletableFuture<Quote>> futures = names.stream()
                .map(v -> venues.get(v).apply(pair)
                        .orTimeout(perVenueTimeout.toMillis(), TimeUnit.MILLISECONDS)
                        .thenApply(rate -> new Quote(v, rate))
                        .exceptionally(ex -> null)) // a failed venue becomes null, not a failed aggregate
                .toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApplyAsync(v -> {
            List<Quote> quotes = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList();
            if (quotes.isEmpty()) throw new IllegalStateException("no venue returned a price for " + pair);
            Quote best = quotes.stream().min(java.util.Comparator.comparing(Quote::rate)).orElseThrow();
            List<String> responded = quotes.stream().map(Quote::venue).sorted().toList();
            List<String> failed = names.stream().filter(n -> !responded.contains(n)).sorted().toList();
            return new BestPrice(best, responded, failed);
        }, executor);
    }

    /** Average across responders (for sanity-checking the best quote). */
    public static BigDecimal average(List<Quote> quotes) {
        if (quotes.isEmpty()) throw new IllegalArgumentException("no quotes");
        BigDecimal sum = quotes.stream().map(Quote::rate).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(quotes.size()), 6, RoundingMode.HALF_EVEN);
    }
}
