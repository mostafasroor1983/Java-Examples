package module12.composition;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * thenApply (map), thenCompose (flatMap), thenCombine (zip two), allOf/anyOf (fan-in), thenAccept/thenRun (side effects).
 * Rule: thenCompose when the function returns a future; thenApply when it returns a value.
 */
public final class Composition {
    private Composition() {
    }

    public static CompletableFuture<BigDecimal> convert(CompletableFuture<BigDecimal> amount, BigDecimal rate) {
        return amount.thenApply(a -> a.multiply(rate));                    // map
    }

    /** BAD: thenApply with an async function yields a nested future. */
    public static CompletableFuture<CompletableFuture<BigDecimal>> nestedBad(CompletableFuture<String> accountId, java.util.function.Function<String, CompletableFuture<BigDecimal>> balanceOf) {
        return accountId.thenApply(balanceOf);
    }

    /** GOOD: thenCompose flattens. */
    public static CompletableFuture<BigDecimal> balanceOf(CompletableFuture<String> accountId, java.util.function.Function<String, CompletableFuture<BigDecimal>> balanceOf) {
        return accountId.thenCompose(balanceOf);                            // flatMap
    }

    /** Two independent calls in parallel, combined when both complete. */
    public static CompletableFuture<BigDecimal> amountInQuote(CompletableFuture<BigDecimal> amount, CompletableFuture<BigDecimal> rate) {
        return amount.thenCombine(rate, BigDecimal::multiply);              // zip
    }

    /** Fan-in: allOf returns Void; collect results via join after it completes (safe: all are done). */
    public static CompletableFuture<BigDecimal> total(List<CompletableFuture<BigDecimal>> balances) {
        return CompletableFuture.allOf(balances.toArray(CompletableFuture[]::new))
                .thenApply(v -> balances.stream().map(CompletableFuture::join).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** First provider to answer wins (anyOf) - type is Object, cast carefully. */
    public static CompletableFuture<BigDecimal> fastestQuote(List<CompletableFuture<BigDecimal>> quotes) {
        return CompletableFuture.anyOf(quotes.toArray(CompletableFuture[]::new)).thenApply(BigDecimal.class::cast);
    }

    /** supplyAsync on a given executor; never rely on the common pool for I/O. */
    public static <T> CompletableFuture<T> async(Supplier<T> body, Executor executor) {
        return CompletableFuture.supplyAsync(body, executor);
    }

    /** Sequential chain built from a list (fold): useful for ordered pipelines. */
    public static CompletableFuture<BigDecimal> applyFeesInOrder(BigDecimal amount, List<java.util.function.Function<BigDecimal, CompletableFuture<BigDecimal>>> fees) {
        CompletableFuture<BigDecimal> acc = CompletableFuture.completedFuture(amount);
        for (var fee : fees) acc = acc.thenCompose(fee);
        return acc;
    }
}
