package module04.ledger;

import module04.ledger.InMemoryDoubleEntryLedger.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryDoubleEntryLedgerTest {
    private static final BigDecimal OPENING = new BigDecimal("1000");
    private static final List<String> IDS = List.of("A", "B", "C", "D");

    private static InMemoryDoubleEntryLedger ledger() {
        var l = new InMemoryDoubleEntryLedger();
        IDS.forEach(id -> l.open(id, OPENING));
        return l;
    }

    private static BigDecimal total(Map<String, BigDecimal> snap) {
        return snap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void postsBothLegsAndHandlesDuplicatesAndFailures() {
        var l = ledger();
        assertEquals(Result.POSTED, l.transfer("T1", "A", "B", new BigDecimal("100")));
        assertEquals(Result.DUPLICATE, l.transfer("T1", "A", "B", new BigDecimal("100")));
        assertEquals(Result.INSUFFICIENT_FUNDS, l.transfer("T2", "A", "B", new BigDecimal("5000")));
        assertEquals(Result.POSTED, l.transfer("T2", "A", "B", new BigDecimal("1")), "failed ref is retryable");
        assertEquals(Result.UNKNOWN_ACCOUNT, l.transfer("T3", "A", "Z", BigDecimal.ONE));
        assertEquals(new BigDecimal("899"), l.snapshot().get("A"));
        assertEquals(2, l.entries("A").size());
        assertTrue(l.entriesExplainBalance("A", OPENING));
        assertThrows(IllegalArgumentException.class, () -> l.transfer("T4", "A", "A", BigDecimal.ONE));
        assertThrows(IllegalStateException.class, () -> l.open("A", BigDecimal.ZERO));
    }

    @Test
    void concurrentRandomTransfersConserveMoneyAndSnapshotsAreAlwaysConsistent() throws Exception {
        var l = ledger();
        BigDecimal expectedTotal = OPENING.multiply(BigDecimal.valueOf(IDS.size()));
        int threads = 12, perThread = 2_000;
        AtomicBoolean done = new AtomicBoolean();
        AtomicInteger inconsistentSnapshots = new AtomicInteger(), snapshots = new AtomicInteger();

        // auditor thread: every snapshot must sum to the constant total, even mid-traffic
        Thread auditor = Thread.ofPlatform().start(() -> {
            while (!done.get()) {
                if (total(l.snapshot()).compareTo(expectedTotal) != 0) inconsistentSnapshots.incrementAndGet();
                snapshots.incrementAndGet();
            }
        });

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        for (int t = 0; t < threads; t++) {
            int tid = t;
            pool.submit(() -> {
                start.await();
                var rnd = ThreadLocalRandom.current();
                for (int i = 0; i < perThread; i++) {
                    String from = IDS.get(rnd.nextInt(IDS.size())), to;
                    do to = IDS.get(rnd.nextInt(IDS.size())); while (to.equals(from));
                    l.transfer("t" + tid + "-" + i, from, to, BigDecimal.valueOf(rnd.nextInt(1, 50)));
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "no deadlock between opposite-direction transfers");
        done.set(true);
        auditor.join();

        Map<String, BigDecimal> finalSnap = l.snapshot();
        assertEquals(0, total(finalSnap).compareTo(expectedTotal), "double entry: money is conserved");
        finalSnap.values().forEach(b -> assertTrue(b.signum() >= 0, "no overdraft"));
        IDS.forEach(id -> assertTrue(l.entriesExplainBalance(id, OPENING), "entries explain balance of " + id));
        assertEquals(0, inconsistentSnapshots.get(), "snapshots never see half a transfer");
        assertTrue(snapshots.get() > 0);
    }

    @Test
    void sameReferenceSubmittedConcurrentlyPostsOnce() throws Exception {
        var l = ledger();
        AtomicInteger posted = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < 64; i++) {
            pool.submit(() -> {
                start.await();
                if (l.transfer("SAME", "A", "B", BigDecimal.TEN) == Result.POSTED) posted.incrementAndGet();
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(1, posted.get());
        assertEquals(new BigDecimal("990"), l.snapshot().get("A"));
    }
}
