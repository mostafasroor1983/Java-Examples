package module15.p17ordering;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class P17Test {

    @Test
    void perAccountOrderPreservedWhileAccountsRunInParallel() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        var ordered = new P17PerKeyOrderedExecutor<String>(pool);
        Map<String, List<Integer>> applied = new ConcurrentHashMap<>();
        AtomicInteger running = new AtomicInteger(), maxParallel = new AtomicInteger();
        List<CompletableFuture<Void>> all = new ArrayList<>();
        int accounts = 16, eventsPerAccount = 500;

        for (int seq = 0; seq < eventsPerAccount; seq++) {
            for (int a = 0; a < accounts; a++) {
                String acc = "ACC-" + a;
                int s = seq;
                all.add(ordered.submit(acc, () -> {
                    maxParallel.accumulateAndGet(running.incrementAndGet(), Math::max);
                    applied.computeIfAbsent(acc, k -> Collections.synchronizedList(new ArrayList<>())).add(s);
                    running.decrementAndGet();
                }));
            }
        }
        CompletableFuture.allOf(all.toArray(CompletableFuture[]::new)).get(30, TimeUnit.SECONDS);
        pool.shutdown();

        for (int a = 0; a < accounts; a++) {
            List<Integer> seen = applied.get("ACC-" + a);
            assertEquals(eventsPerAccount, seen.size());
            for (int i = 0; i < seen.size(); i++) assertEquals(i, seen.get(i), "out of order on ACC-" + a);
        }
        assertTrue(maxParallel.get() > 1, "different accounts ran concurrently");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (ordered.activeKeys() > 0 && System.nanoTime() < deadline) Thread.onSpinWait();
        assertEquals(0, ordered.activeKeys(), "idle keys are removed: no memory leak");
    }

    @Test
    void failingEventDoesNotBlockLaterEventsForSameAccount() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        var ordered = new P17PerKeyOrderedExecutor<String>(pool);
        List<String> log = Collections.synchronizedList(new ArrayList<>());
        var f1 = ordered.submit("A", () -> log.add("deposit"));
        var f2 = ordered.submit("A", () -> { throw new IllegalStateException("bad event"); });
        var f3 = ordered.submit("A", () -> log.add("withdraw"));
        f3.get(5, TimeUnit.SECONDS);
        assertTrue(f1.isDone() && !f1.isCompletedExceptionally());
        assertTrue(f2.isCompletedExceptionally(), "failure reported to its submitter");
        assertEquals(List.of("deposit", "withdraw"), log);
        pool.shutdown();
    }
}
