package module14.contention;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Lock contention: one coarse lock serialises everything; striping by key (or using CHM's per-bin locking)
 * lets independent accounts proceed in parallel. Also: shrink the critical section (compute outside, publish inside).
 */
public final class LockContentionDemo {
    private LockContentionDemo() {
    }

    public interface BalanceStore {
        void credit(String account, BigDecimal amount);

        BigDecimal balance(String account);
    }

    /** BAD: one global lock for all accounts. */
    public static final class CoarseLocked implements BalanceStore {
        private final java.util.Map<String, BigDecimal> balances = new java.util.HashMap<>();

        public synchronized void credit(String account, BigDecimal amount) {
            balances.merge(account, amount, BigDecimal::add);
        }

        public synchronized BigDecimal balance(String account) {
            return balances.getOrDefault(account, BigDecimal.ZERO);
        }
    }

    /** GOOD: lock striping - accounts hash to one of N locks. */
    public static final class Striped implements BalanceStore {
        private final ReentrantLock[] locks;
        private final java.util.Map<String, BigDecimal>[] shards;

        @SuppressWarnings("unchecked")
        public Striped(int stripes) {
            locks = new ReentrantLock[stripes];
            shards = new java.util.Map[stripes];
            for (int i = 0; i < stripes; i++) {
                locks[i] = new ReentrantLock();
                shards[i] = new java.util.HashMap<>();
            }
        }

        private int stripe(String account) {
            return Math.floorMod(account.hashCode(), locks.length);
        }

        public void credit(String account, BigDecimal amount) {
            int s = stripe(account);
            locks[s].lock();
            try {
                shards[s].merge(account, amount, BigDecimal::add);
            } finally {
                locks[s].unlock();
            }
        }

        public BigDecimal balance(String account) {
            int s = stripe(account);
            locks[s].lock();
            try {
                return shards[s].getOrDefault(account, BigDecimal.ZERO);
            } finally {
                locks[s].unlock();
            }
        }
    }

    /** BEST for this shape: CHM does the striping for you with per-bin locking. */
    public static final class Chm implements BalanceStore {
        private final ConcurrentHashMap<String, BigDecimal> balances = new ConcurrentHashMap<>();

        public void credit(String account, BigDecimal amount) {
            balances.merge(account, amount, BigDecimal::add);
        }

        public BigDecimal balance(String account) {
            return balances.getOrDefault(account, BigDecimal.ZERO);
        }
    }

    /** Hammer: `threads` threads each credit `perThread` times, each thread to its own account. Returns nanos. */
    public static long hammer(BalanceStore store, int threads, int perThread) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            String account = "acc-" + t;
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) store.credit(account, BigDecimal.ONE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        long t0 = System.nanoTime();
        start.countDown();
        done.await();
        return System.nanoTime() - t0;
    }
}
