package module01.encapsulation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BankAccountTest {

    @Test
    void depositAndWithdrawKeepInvariant() {
        BankAccount acc = new BankAccount("A1", new BigDecimal("100"));
        acc.deposit(new BigDecimal("50"));
        acc.withdraw(new BigDecimal("30"));
        assertEquals(new BigDecimal("120"), acc.balance());
        assertEquals(3, acc.history().size());
    }

    @Test
    void cannotOverdraw() {
        BankAccount acc = new BankAccount("A1", new BigDecimal("10"));
        assertThrows(InsufficientFundsException.class, () -> acc.withdraw(new BigDecimal("10.01")));
        assertEquals(new BigDecimal("10"), acc.balance());
    }

    @Test
    void historyIsDefensiveCopy() {
        BankAccount acc = new BankAccount("A1", BigDecimal.ONE);
        List<String> h = acc.history(); // return unmodefiable copy of the list
        assertThrows(UnsupportedOperationException.class, () -> h.add("HACK"));
        assertEquals(1, acc.history().size());
        assertEquals(1, h.size());
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> new BankAccount(" ", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new BankAccount("A", new BigDecimal("-1")));
        BankAccount acc = new BankAccount("A", BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> acc.deposit(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> acc.withdraw(null));
    }


    // Concurrent threads are depositing money, each one 1$ => balance = number of deposit call
    @Test
    void concurrentDepositsAreExact() throws Exception {
        BankAccount acc = new BankAccount("A1", BigDecimal.ZERO);
        int threads = 8, perThread = 100000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) acc.deposit(BigDecimal.ONE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(new BigDecimal(threads * perThread), acc.balance());
    }


    /*Concurrent threads are depositing & withdrawing money, each one 1$ added and 1$ subtracted => balance = 0.
    There is no race condition in this case, because if we have thread on deposit the entire bank account object
    will be locked => no another thread on withdraw method.
     */
    @Test
    void concurrentDepositsAndWithdrawalsBalanceIsZero() throws Exception {
        BankAccount acc = new BankAccount("A1", BigDecimal.ZERO);
        int threads = 8, perThread = 100000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        acc.withdraw(BigDecimal.ONE);
                        acc.deposit(BigDecimal.ONE);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(BigDecimal.ZERO, acc.balance());
    }

}
