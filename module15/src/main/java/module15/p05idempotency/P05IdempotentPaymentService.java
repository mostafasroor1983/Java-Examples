package module15.p05idempotency;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * P05 - Idempotent payment submission: same key + same payload => same result, executed once.
 * Same key + different payload => conflict (client bug). Concurrent duplicates wait on the same in-flight future.
 */
public final class P05IdempotentPaymentService {
    public record Request(String idempotencyKey, String accountId, BigDecimal amount) {
    }

    public record Response(String paymentId, String status) {
    }

    public static final class ConflictException extends RuntimeException {
        public ConflictException(String key) {
            super("idempotency key reused with a different payload: " + key);
        }
    }

    private record Stored(Request request, java.util.concurrent.CompletableFuture<Response> result) {
    }

    private final ConcurrentHashMap<String, Stored> store = new ConcurrentHashMap<>();
    private final AtomicInteger executions = new AtomicInteger();
    private final AtomicInteger sequence = new AtomicInteger();

    public Response submit(Request req) {
        Objects.requireNonNull(req.idempotencyKey());
        boolean[] creator = {false};
        Stored stored = store.computeIfAbsent(req.idempotencyKey(), k -> {
            creator[0] = true;
            return new Stored(req, new java.util.concurrent.CompletableFuture<>());
        });
        if (!creator[0]) {
            if (!stored.request().equals(req)) throw new ConflictException(req.idempotencyKey());
            return stored.result().join(); // wait for / read the first caller's result
        }
        try {
            Response r = execute(req);
            stored.result().complete(r);
            return r;
        } catch (RuntimeException e) {
            store.remove(req.idempotencyKey(), stored); // allow retry after a failure
            stored.result().completeExceptionally(e);
            throw e;
        }
    }

    private Response execute(Request req) {
        executions.incrementAndGet();
        if (req.amount().signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        return new Response("pay-" + sequence.incrementAndGet(), "ACCEPTED");
    }

    public int executions() {
        return executions.get();
    }
}
