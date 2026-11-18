package module01.hexagonal;

import module01.immutability.Money;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TransferMoneyServiceTest {

    private static InMemoryAccountRepository repo(String a, String b) {
        InMemoryAccountRepository r = new InMemoryAccountRepository();
        r.seed(new Account("A", Money.of(a, "EUR"), 0));
        r.seed(new Account("B", Money.of(b, "EUR"), 0));
        return r;
    }

    private static Money balance(AccountRepository r, String id) {
        return r.findById(id).orElseThrow().balance();
    }

    @Test
    void transferMovesMoneyAndBumpsVersions() {
        InMemoryAccountRepository r = repo("100", "0");
        var service = new TransferMoneyService(r, 3);
        assertEquals(TransferMoneyService.Outcome.COMPLETED,
                service.transfer(new TransferCommand("k1", "A", "B", Money.of("40", "EUR"))));
        assertEquals(Money.of("60", "EUR"), balance(r, "A"));
        assertEquals(Money.of("40", "EUR"), balance(r, "B"));
        assertEquals(1, r.findById("A").orElseThrow().version());
    }

    @Test
    void retryWithSameKeyIsADuplicateNotASecondTransfer() {
        InMemoryAccountRepository r = repo("100", "0");
        var service = new TransferMoneyService(r, 3);
        var cmd = new TransferCommand("k1", "A", "B", Money.of("40", "EUR"));
        service.transfer(cmd);
        assertEquals(TransferMoneyService.Outcome.DUPLICATE, service.transfer(cmd));
        assertEquals(Money.of("60", "EUR"), balance(r, "A"));
    }

    @Test
    void insufficientFundsLeavesStateUntouched() {
        InMemoryAccountRepository r = repo("10", "0");
        var service = new TransferMoneyService(r, 3);
        assertThrows(Account.InsufficientFundsException.class,
                () -> service.transfer(new TransferCommand("k1", "A", "B", Money.of("10.01", "EUR"))));
        assertEquals(Money.of("10", "EUR"), balance(r, "A"));
        assertFalse(r.isProcessed("k1"), "a failed command must be retryable");
    }

    @Test
    void persistentConflictIsSurfacedAfterBoundedRetries() {
        AtomicInteger commits = new AtomicInteger();
        InMemoryAccountRepository real = repo("100", "0");
        AccountRepository alwaysConflicting = new AccountRepository() {
            public Optional<Account> findById(String id) { return real.findById(id); }
            public boolean isProcessed(String key) { return false; }
            public void commit(List<Account> updated, String key) {
                commits.incrementAndGet();
                throw new OptimisticLockException("A");
            }
        };
        var service = new TransferMoneyService(alwaysConflicting, 3);
        assertThrows(AccountRepository.OptimisticLockException.class,
                () -> service.transfer(new TransferCommand("k1", "A", "B", Money.of("1", "EUR"))));
        assertEquals(3, commits.get());
    }

    @Test
    void concurrentTransfersConserveMoneyAndApplyEachKeyOnce() throws Exception {
        InMemoryAccountRepository r = repo("1000", "1000");
        var service = new TransferMoneyService(r, 1_000);
        int threads = 8, perThread = 50;
        AtomicInteger completed = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        for (int t = 0; t < threads; t++) {
            int id = t;
            pool.submit(() -> {
                start.await();
                for (int i = 0; i < perThread; i++) {
                    // every key is submitted twice (client retry) and directions alternate
                    String key = "t" + id + "-" + i;
                    boolean ab = (id + i) % 2 == 0;
                    var cmd = new TransferCommand(key, ab ? "A" : "B", ab ? "B" : "A", Money.of("1", "EUR"));
                    if (service.transfer(cmd) == TransferMoneyService.Outcome.COMPLETED) completed.incrementAndGet();
                    service.transfer(cmd);
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertEquals(threads * perThread, completed.get(), "each key completes exactly once");
        assertEquals(Money.of("2000", "EUR"), balance(r, "A").plus(balance(r, "B")), "money is conserved");
    }
}
