package module12.priceservice;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class PriceServiceTest {
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    private static Function<String, CompletableFuture<BigDecimal>> venue(String rate) {
        return pair -> CompletableFuture.completedFuture(new BigDecimal(rate));
    }

    @Test
    void picksBestAndReportsFailures() {
        CompletableFuture<BigDecimal> hanging = new CompletableFuture<>();
        PriceService svc = new PriceService(Map.of(
                "LMAX", venue("1.2510"),
                "EBS", venue("1.2495"),
                "SLOW", pair -> hanging,
                "DOWN", pair -> CompletableFuture.failedFuture(new IllegalStateException("503"))),
                pool, Duration.ofMillis(100));
        var best = svc.bestPrice("GBPUSD").join();
        assertEquals("EBS", best.best().venue());
        assertEquals(List.of("EBS", "LMAX"), best.respondedVenues());
        assertEquals(List.of("DOWN", "SLOW"), best.failedVenues());
        pool.shutdown();
    }

    @Test
    void allVenuesFailingIsAnError() {
        PriceService svc = new PriceService(Map.of("DOWN", pair -> CompletableFuture.failedFuture(new RuntimeException())), pool, Duration.ofMillis(100));
        CompletionException e = assertThrows(CompletionException.class, () -> svc.bestPrice("GBPUSD").join());
        assertInstanceOf(IllegalStateException.class, e.getCause());
        assertEquals(new BigDecimal("1.250000"), PriceService.average(List.of(new PriceService.Quote("a", new BigDecimal("1.25")), new PriceService.Quote("b", new BigDecimal("1.25")))));
        assertThrows(IllegalArgumentException.class, () -> PriceService.average(List.of()));
        pool.shutdown();
    }
}
