package module12.orderfulfilment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Card order fulfilment: in parallel fetch customer, check KYC, reserve card stock; then (sequentially)
 * charge the fee and dispatch. Illustrates mixing parallel fan-out with a dependent sequential tail.
 */
public final class OrderFulfilment {
    public record Customer(String id, String tier) {
    }

    public record KycResult(boolean passed, String reason) {
    }

    public record Reservation(String sku, int remaining) {
    }

    public record Dispatch(String orderId, String customerId, String sku, BigDecimal feeCharged, String trackingRef) {
    }

    public interface Ports {
        CompletableFuture<Customer> customer(String id);

        CompletableFuture<KycResult> kyc(String customerId);

        CompletableFuture<Reservation> reserve(String sku);

        CompletableFuture<BigDecimal> chargeFee(String customerId, String tier);

        CompletableFuture<String> dispatch(String orderId, String sku);

        CompletableFuture<Void> releaseReservation(String sku);
    }

    public static final class KycFailedException extends RuntimeException {
        public KycFailedException(String reason) {
            super("KYC failed: " + reason);
        }
    }

    private final Ports ports;
    private final Executor executor;

    public OrderFulfilment(Ports ports, Executor executor) {
        this.ports = Objects.requireNonNull(ports);
        this.executor = Objects.requireNonNull(executor);
    }

    public CompletableFuture<Dispatch> fulfil(String orderId, String customerId, String sku) {
        CompletableFuture<Customer> customer = ports.customer(customerId);
        CompletableFuture<KycResult> kyc = ports.kyc(customerId);
        CompletableFuture<Reservation> reservation = ports.reserve(sku);

        return CompletableFuture.allOf(customer, kyc, reservation)
                .thenComposeAsync(v -> {
                    KycResult k = kyc.join();
                    if (!k.passed()) {
                        // compensate: release stock, then fail
                        return ports.releaseReservation(sku).thenCompose(ignored ->
                                CompletableFuture.<Dispatch>failedFuture(new KycFailedException(k.reason())));
                    }
                    Customer c = customer.join();
                    return ports.chargeFee(customerId, c.tier())
                            .thenCompose(fee -> ports.dispatch(orderId, sku)
                                    .thenApply(tracking -> new Dispatch(orderId, customerId, sku, fee, tracking)));
                }, executor);
    }

    /** Fulfil many orders concurrently; failures are reported per order, not as a whole. */
    public CompletableFuture<List<String>> fulfilAll(List<String[]> orders) {
        List<CompletableFuture<String>> results = orders.stream()
                .map(o -> fulfil(o[0], o[1], o[2]).handle((d, ex) -> ex == null ? "OK " + d.orderId() : "FAILED " + o[0] + ": " + rootMessage(ex)))
                .toList();
        return CompletableFuture.allOf(results.toArray(CompletableFuture[]::new))
                .thenApply(v -> results.stream().map(CompletableFuture::join).toList());
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage();
    }
}
