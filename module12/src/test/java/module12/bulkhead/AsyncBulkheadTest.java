package module12.bulkhead;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AsyncBulkheadTest {

    @Test
    void capsInFlightAndFailsFastWhenFull() {
        var bh = new AsyncBulkhead("card-scheme", 3);
        List<CompletableFuture<String>> pending = new ArrayList<>();
        List<CompletableFuture<String>> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CompletableFuture<String> downstream = new CompletableFuture<>();
            results.add(bh.call(() -> downstream));
            pending.add(downstream);
        }
        assertEquals(3, bh.inFlight());
        assertEquals(7, bh.rejectedCount());
        var ex = assertThrows(ExecutionException.class, () -> results.get(9).get(1, TimeUnit.SECONDS));
        assertInstanceOf(AsyncBulkhead.BulkheadFullException.class, ex.getCause());

        pending.get(0).complete("ok");
        pending.get(1).completeExceptionally(new IllegalStateException("502"));
        pending.get(2).cancel(true);
        assertEquals(0, bh.inFlight(), "permits released on success, failure and cancellation");
    }

    @Test
    void supplierThrowingDoesNotLeakPermit() {
        var bh = new AsyncBulkhead("kyc", 1);
        var f = bh.<String>call(() -> { throw new IllegalArgumentException("bad request"); });
        assertTrue(f.isCompletedExceptionally());
        assertEquals(0, bh.inFlight());
        assertEquals("x", bh.call(() -> CompletableFuture.completedFuture("x")).join());
    }

    @Test
    void manyConcurrentCallersNeverExceedLimit() throws Exception {
        var bh = new AsyncBulkhead("fx", 5);
        var maxSeen = new java.util.concurrent.atomic.AtomicInteger();
        List<CompletableFuture<Integer>> all = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            all.add(bh.call(() -> CompletableFuture.supplyAsync(() -> {
                maxSeen.accumulateAndGet(bh.inFlight(), Math::max);
                return 1;
            })).exceptionally(t -> 0));
        }
        CompletableFuture.allOf(all.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
        assertTrue(maxSeen.get() <= 5);
        int ok = all.stream().mapToInt(CompletableFuture::join).sum();
        assertEquals(500, ok + bh.rejectedCount());
        assertEquals(0, bh.inFlight());
    }
}
