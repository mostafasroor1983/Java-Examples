package module18;

import module18.ch11.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** Chapter 11 - Concurrency (items 78-84). */
class Chapter11Test {

    private static void runConcurrently(int threads, Runnable body) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < threads; i++) pool.submit(() -> { start.await(); body.run(); return null; });
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));
    }

    @Test
    void item78_volatileStopFlagAndAtomicCounter() throws Exception {
        var worker = new Item78SynchronizeSharedData.SettlementWorker();
        Thread t = new Thread(worker);
        t.start();
        while (worker.processed() == 0) Thread.onSpinWait();
        worker.requestStop();
        t.join(5_000);
        assertFalse(t.isAlive(), "worker saw the volatile stop flag");

        var safe = new Item78SynchronizeSharedData.SafeSerial();
        Set<Long> seen = ConcurrentHashMap.newKeySet();
        runConcurrently(8, () -> { for (int i = 0; i < 10_000; i++) seen.add(safe.next()); });
        assertEquals(80_000, seen.size(), "no duplicate serial numbers");
    }

    @Test
    void item79_openCallsAllowSelfUnsubscribe() {
        var ledger = new Item79ExcessiveSynchronization.ObservableLedger();
        List<Long> seen = new ArrayList<>();
        var self = new java.util.concurrent.atomic.AtomicReference<Consumer<Long>>();
        self.set(b -> { seen.add(b); ledger.unsubscribe(self.get()); }); // modifies listeners during notification
        ledger.subscribe(self.get());
        ledger.post(100);
        ledger.post(50);
        assertEquals(List.of(100L), seen);
        assertEquals(0, ledger.listenerCount());
    }

    @Test
    void item80_poolAndVirtualThreadsGiveSameResults() throws Exception {
        var accounts = List.of("A", "B", "C");
        java.util.function.Function<String, BigDecimal> lookup = a -> new BigDecimal(a.charAt(0) - 'A' + 1);
        assertEquals(Item80Executors.fetchBalancesPool(accounts, lookup), Item80Executors.fetchBalancesVirtual(accounts, lookup));
        var many = Collections.nCopies(2_000, "A");
        assertEquals(2_000, Item80Executors.fetchBalancesVirtual(many, a -> {
            try {
                Thread.sleep(5); // blocking I/O stand-in: cheap on virtual threads
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return BigDecimal.ONE;
        }).size());
    }

    @Test
    void item81_concurrencyUtilities() throws Exception {
        String a = Item81ConcurrencyUtilities.canonical(new String("EUR"));
        String b = Item81ConcurrencyUtilities.canonical(new String("EUR"));
        assertSame(a, b);
        AtomicInteger settled = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        long nanos = Item81ConcurrencyUtilities.timeConcurrentSettlement(pool, 4, settled::incrementAndGet);
        pool.shutdown();
        assertEquals(4, settled.get());
        assertTrue(nanos >= 0);
    }

    @Test
    void item82_documentedThreadSafeClassIsThreadSafe() throws Exception {
        var statement = new Item82DocumentThreadSafety.AccountStatement();
        runConcurrently(8, () -> { for (int i = 0; i < 1_000; i++) statement.add(BigDecimal.ONE); });
        assertEquals(8_000, statement.snapshot().size());
        assertEquals(List.of("a", "b"), new Item82DocumentThreadSafety.BatchBuilder().add("a").add("b").build());
    }

    @Test
    void item83_lazyInitialisedExactlyOnce() throws Exception {
        assertEquals("BNP Paribas", Item83LazyInitialization.bicDirectory().get("BNPAFRPP"));
        Item83LazyInitialization.bicDirectory();
        assertEquals(1, Item83LazyInitialization.HOLDER_INITIALISATIONS.get());
        var model = new Item83LazyInitialization.FraudModel();
        runConcurrently(16, () -> { for (int i = 0; i < 100; i++) model.weights(); });
        assertEquals(1, model.loads());
    }

    @Test
    void item84_blockInsteadOfSpinning() throws Exception {
        var files = new CountDownLatch(2);
        new Thread(() -> { files.countDown(); files.countDown(); }).start();
        assertTrue(Item84ThreadScheduler.awaitSettlementFiles(files, 5_000));
        assertFalse(Item84ThreadScheduler.awaitSettlementFiles(new CountDownLatch(1), 20), "times out instead of hanging");
        var spinning = new Item84ThreadScheduler.SpinningLatch(1);
        spinning.countDown();
        spinning.await(); // returns immediately here; under contention it burns a core
    }
}
