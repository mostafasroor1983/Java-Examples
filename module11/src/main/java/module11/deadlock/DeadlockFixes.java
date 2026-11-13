package module11.deadlock;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fix 1: global lock ordering (by account id).
 * Fix 2: tryLock with timeout + back-off (no ordering needed, but may livelock without jitter - see module11.livelock).
 * Fix 3: a single coarse lock (simplest, least concurrent).
 */
public final class DeadlockFixes {
    private DeadlockFixes() {
    }

    public static final class Account {
        final String id;
        final ReentrantLock lock = new ReentrantLock();
        BigDecimal balance;

        public Account(String id, BigDecimal balance) {
            this.id = id;
            this.balance = balance;
        }

        public BigDecimal balance() {
            lock.lock();
            try {
                return balance;
            } finally {
                lock.unlock();
            }
        }
    }

    /** Fix 1: always lock the lower id first. */
    public static void transferOrdered(Account from, Account to, BigDecimal amount) {
        Account first = from.id.compareTo(to.id) < 0 ? from : to;
        Account second = first == from ? to : from;
        first.lock.lock();
        try {
            second.lock.lock();
            try {
                move(from, to, amount);
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }

    /** Fix 2: tryLock both; on failure release everything and retry. Returns false after maxAttempts. */
    public static boolean transferTryLock(Account from, Account to, BigDecimal amount, int maxAttempts) throws InterruptedException {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (from.lock.tryLock(5, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(5, TimeUnit.MILLISECONDS)) {
                        try {
                            move(from, to, amount);
                            return true;
                        } finally {
                            to.lock.unlock();
                        }
                    }
                } finally {
                    from.lock.unlock();
                }
            }
            Thread.sleep(0, 1000 * (1 + (int) (Math.random() * 50))); // jittered back-off (<= 50 microseconds)
        }
        return false;
    }

    private static void move(Account from, Account to, BigDecimal amount) {
        if (from.balance.compareTo(amount) < 0) return; // insufficient: no-op for the demo
        from.balance = from.balance.subtract(amount);
        to.balance = to.balance.add(amount);
    }
}
