package module12.composition;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class CompositionTest {
    private static CompletableFuture<BigDecimal> done(String v) {
        return CompletableFuture.completedFuture(new BigDecimal(v));
    }

    @Test
    void applyComposeCombine() {
        assertEquals(new BigDecimal("125.00"), Composition.convert(done("100"), new BigDecimal("1.25")).join());
        Function<String, CompletableFuture<BigDecimal>> balanceOf = id -> done("42");
        assertEquals(new BigDecimal("42"), Composition.balanceOf(CompletableFuture.completedFuture("A"), balanceOf).join());
        assertEquals(new BigDecimal("42"), Composition.nestedBad(CompletableFuture.completedFuture("A"), balanceOf).join().join(), "nested future needs a double join");
        assertEquals(new BigDecimal("250.0"), Composition.amountInQuote(done("100"), done("2.5")).join());
    }

    @Test
    void fanInAndFanOut() {
        assertEquals(new BigDecimal("6"), Composition.total(List.of(done("1"), done("2"), done("3"))).join());
        assertEquals(BigDecimal.ZERO, Composition.total(List.of()).join());
        CompletableFuture<BigDecimal> never = new CompletableFuture<>();
        assertEquals(new BigDecimal("7"), Composition.fastestQuote(List.of(never, done("7"))).join());
    }

    @Test
    void asyncOnExecutorAndOrderedFold() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2, r -> new Thread(r, "fx-io"));
        try {
            assertEquals("fx-io", Composition.async(() -> Thread.currentThread().getName(), pool).join());
            var fees = List.<Function<BigDecimal, CompletableFuture<BigDecimal>>>of(
                    a -> CompletableFuture.supplyAsync(() -> a.subtract(BigDecimal.ONE), pool),
                    a -> CompletableFuture.supplyAsync(() -> a.multiply(BigDecimal.TEN), pool));
            assertEquals(new BigDecimal("90"), Composition.applyFeesInOrder(BigDecimal.TEN, fees).join(), "(10-1)*10, order matters");
        } finally {
            pool.shutdownNow();
        }
    }
}
