package module01.exercise1;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void creditDebitRecordsMovements() {
        Wallet w = new Wallet(new WalletId("w1"), GBP);
        w.credit(new BigDecimal("100.00"), GBP, "top-up");
        w.debit(new BigDecimal("40.00"), GBP, "coffee");
        assertEquals(new BigDecimal("60.00"), w.balance());
        assertEquals(2, w.movements().size());
        assertEquals(Wallet.Direction.DEBIT, w.movements().get(1).direction());
    }

    @Test
    void cannotGoNegative() {
        Wallet w = new Wallet(new WalletId("w1"), GBP);
        w.credit(new BigDecimal("10.00"), GBP, "x");
        assertThrows(IllegalStateException.class, () -> w.debit(new BigDecimal("10.01"), GBP, "y"));
        assertEquals(new BigDecimal("10.00"), w.balance());
        assertEquals(1, w.movements().size(), "failed debit must not be recorded");
    }

    @Test
    void rejectsCrossCurrencyAndBadInput() {
        Wallet w = new Wallet(new WalletId("w1"), GBP);
        assertThrows(IllegalArgumentException.class, () -> w.credit(BigDecimal.ONE, EUR, "x"));
        assertThrows(IllegalArgumentException.class, () -> w.credit(BigDecimal.ZERO, GBP, "x"));
        assertThrows(IllegalArgumentException.class, () -> w.credit(BigDecimal.ONE, GBP, " "));
        assertThrows(IllegalArgumentException.class, () -> new WalletId(""));
    }

    @Test
    void concurrentCreditsAndDebitsBalanceExactly() throws Exception {
        Wallet w = new Wallet(new WalletId("w1"), GBP);
        w.credit(new BigDecimal("100000.00"), GBP, "seed");
        int threads = 10, ops = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            boolean credit = t % 2 == 0;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < ops; i++) {
                        if (credit) w.credit(new BigDecimal("2.00"), GBP, "c");
                        else w.debit(new BigDecimal("1.00"), GBP, "d");
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
        // 5 crediting threads * 500 * 2 = 5000 ; 5 debiting threads * 500 * 1 = 2500
        assertEquals(new BigDecimal("102500.00"), w.balance());
        assertEquals(1 + threads * ops, w.movements().size());
    }
}
