package module11.deadlock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Classic deadlock: two transfers lock accounts in opposite order.
 * The demo uses latches to force the interleaving deterministically, then detects the deadlock with ThreadMXBean
 * and interrupts nothing (intrinsic locks cannot be interrupted) - the threads are left as daemons.
 */
public final class DeadlockDemo {
    private DeadlockDemo() {
    }

    public static final class Account {
        final String id;
        BigDecimal balance;

        public Account(String id, BigDecimal balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    /** BAD: lock order depends on argument order. */
    public static void transferBad(Account from, Account to, BigDecimal amount, CountDownLatch gotFirst, CountDownLatch proceed) {
        synchronized (from) {
            gotFirst.countDown();
            try {
                proceed.await(2, TimeUnit.SECONDS); // give the other thread time to grab its first lock
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (to) {
                from.balance = from.balance.subtract(amount);
                to.balance = to.balance.add(amount);
            }
        }
    }

    /** Runs A->B and B->A concurrently with the bad method and reports whether the JVM detected a deadlock. */
    public static boolean reproduceDeadlock() throws InterruptedException {
        Account a = new Account("A", new BigDecimal("100"));
        Account b = new Account("B", new BigDecimal("100"));
        CountDownLatch bothHoldFirst = new CountDownLatch(2);
        CountDownLatch proceed = new CountDownLatch(1);
        Thread t1 = daemon(() -> transferBad(a, b, BigDecimal.ONE, bothHoldFirst, proceed));
        Thread t2 = daemon(() -> transferBad(b, a, BigDecimal.ONE, bothHoldFirst, proceed));
        t1.start();
        t2.start();
        bothHoldFirst.await(5, TimeUnit.SECONDS);
        proceed.countDown();
        // Both now try to take the other lock.
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        for (int i = 0; i < 200; i++) {
            long[] ids = mx.findDeadlockedThreads();
            if (ids != null && ids.length >= 2) return true;
            Thread.sleep(10);
        }
        return false;
    }

    private static Thread daemon(Runnable r) {
        Thread t = new Thread(r, "deadlock-demo");
        t.setDaemon(true); // never block JVM exit
        return t;
    }
}
