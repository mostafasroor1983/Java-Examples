package module15.p03transfer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class P03Test {
    @Test
    void resultsAndValidation() {
        P03MoneyTransfer t = new P03MoneyTransfer();
        t.open("A", new BigDecimal("100"));
        t.open("B", BigDecimal.ZERO);
        assertEquals(P03MoneyTransfer.Result.OK, t.transfer("A", "B", new BigDecimal("30")));
        assertEquals(P03MoneyTransfer.Result.INSUFFICIENT_FUNDS, t.transfer("A", "B", new BigDecimal("500")));
        assertEquals(P03MoneyTransfer.Result.UNKNOWN_ACCOUNT, t.transfer("A", "Z", BigDecimal.ONE));
        assertEquals(P03MoneyTransfer.Result.SAME_ACCOUNT, t.transfer("A", "A", BigDecimal.ONE));
        assertEquals(P03MoneyTransfer.Result.INVALID_AMOUNT, t.transfer("A", "B", BigDecimal.ZERO));
        assertEquals(new BigDecimal("70"), t.balance("A"));
        assertThrows(IllegalStateException.class, () -> t.open("A", BigDecimal.ONE));
    }

    @Test
    void concurrentOpposingTransfersConserveMoneyWithoutDeadlock() throws Exception {
        P03MoneyTransfer t = new P03MoneyTransfer();
        for (String id : new String[]{"A", "B", "C"}) t.open(id, new BigDecimal("1000"));
        String[][] pairs = {{"A", "B"}, {"B", "C"}, {"C", "A"}, {"B", "A"}, {"C", "B"}, {"A", "C"}};
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(pairs.length);
        for (String[] p : pairs) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 2000; i++) t.transfer(p[0], p[1], BigDecimal.ONE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "no deadlock");
        assertEquals(new BigDecimal("3000"), t.totalMoney());
        for (String id : new String[]{"A", "B", "C"}) assertTrue(t.balance(id).signum() >= 0);
    }
}
