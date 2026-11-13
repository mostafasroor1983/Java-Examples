package module11.locks;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock over synchronized when you need: tryLock with timeout, interruptible lock, fairness,
 * multiple Conditions, or lock/unlock across method boundaries. Always unlock in finally.
 */
public final class ReentrantLockAccount {
    private final ReentrantLock lock;
    private final Condition fundsAvailable;
    private BigDecimal balance;

    public ReentrantLockAccount(BigDecimal opening, boolean fair) {
        this.lock = new ReentrantLock(fair);
        this.fundsAvailable = lock.newCondition();
        this.balance = opening;
    }

    public void deposit(BigDecimal amount) {
        lock.lock();
        try {
            balance = balance.add(amount);
            fundsAvailable.signalAll(); // wake waiters blocked in awaitWithdraw
        } finally {
            lock.unlock();
        }
    }

    /** Non-blocking attempt: returns false instead of waiting if the lock is busy or funds are short. */
    public boolean tryWithdraw(BigDecimal amount, long timeout, TimeUnit unit) throws InterruptedException {
        if (!lock.tryLock(timeout, unit)) return false;
        try {
            if (balance.compareTo(amount) < 0) return false;
            balance = balance.subtract(amount);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /** Condition: wait until funds are available or timeout. The classic use case a monitor cannot express cleanly with multiple conditions. */
    public boolean awaitWithdraw(BigDecimal amount, long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (balance.compareTo(amount) < 0) {
                if (nanos <= 0) return false;
                nanos = fundsAvailable.awaitNanos(nanos);
            }
            balance = balance.subtract(amount);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /** Reentrancy: the same thread can take the lock again (audit calls balance while holding it). */
    public String audit() {
        lock.lock();
        try {
            return "balance=" + balance() + " holds=" + lock.getHoldCount();
        } finally {
            lock.unlock();
        }
    }

    public BigDecimal balance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    public boolean isFair() {
        return lock.isFair();
    }
}
