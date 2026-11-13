package module12.orderfulfilment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OrderFulfilmentTest {
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    private OrderFulfilment.Ports ports(boolean kycPass, AtomicInteger released) {
        return new OrderFulfilment.Ports() {
            public CompletableFuture<OrderFulfilment.Customer> customer(String id) {
                return CompletableFuture.supplyAsync(() -> new OrderFulfilment.Customer(id, "PREMIUM"), pool);
            }

            public CompletableFuture<OrderFulfilment.KycResult> kyc(String customerId) {
                return CompletableFuture.supplyAsync(() -> new OrderFulfilment.KycResult(kycPass, kycPass ? "" : "sanctions hit"), pool);
            }

            public CompletableFuture<OrderFulfilment.Reservation> reserve(String sku) {
                return CompletableFuture.supplyAsync(() -> new OrderFulfilment.Reservation(sku, 9), pool);
            }

            public CompletableFuture<BigDecimal> chargeFee(String customerId, String tier) {
                return CompletableFuture.completedFuture(tier.equals("PREMIUM") ? BigDecimal.ZERO : new BigDecimal("4.99"));
            }

            public CompletableFuture<String> dispatch(String orderId, String sku) {
                return CompletableFuture.completedFuture("TRK-" + orderId);
            }

            public CompletableFuture<Void> releaseReservation(String sku) {
                released.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        };
    }

    @Test
    void happyPath() {
        AtomicInteger released = new AtomicInteger();
        var d = new OrderFulfilment(ports(true, released), pool).fulfil("o1", "c1", "CARD-METAL").join();
        assertEquals("TRK-o1", d.trackingRef());
        assertEquals(BigDecimal.ZERO, d.feeCharged());
        assertEquals(0, released.get());
        pool.shutdown();
    }

    @Test
    void kycFailureCompensatesAndFails() {
        AtomicInteger released = new AtomicInteger();
        var f = new OrderFulfilment(ports(false, released), pool);
        CompletionException e = assertThrows(CompletionException.class, () -> f.fulfil("o2", "c2", "CARD").join());
        assertInstanceOf(OrderFulfilment.KycFailedException.class, e.getCause());
        assertEquals(1, released.get());
        pool.shutdown();
    }

    @Test
    void batchReportsPerOrder() {
        AtomicInteger released = new AtomicInteger();
        var ok = new OrderFulfilment(ports(true, released), pool);
        var results = ok.fulfilAll(List.of(new String[]{"o1", "c1", "S"}, new String[]{"o2", "c2", "S"})).join();
        assertEquals(List.of("OK o1", "OK o2"), results);
        var bad = new OrderFulfilment(ports(false, released), pool);
        assertEquals("FAILED o3: KYC failed: sanctions hit", bad.fulfilAll(List.<String[]>of(new String[]{"o3", "c3", "S"})).join().get(0));
        pool.shutdown();
    }
}
