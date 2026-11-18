package module11.conditions;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A payout account where an outgoing payment may WAIT for incoming funds until a deadline (e.g. a merchant payout
 * waits for the card-scheme settlement to land before the 16:00 cut-off).
 *
 * <p>Pattern: {@link ReentrantLock} + {@link Condition}. Compared with {@code synchronized}/{@code wait}/{@code notifyAll}:
 * timed waits that return the remaining time ({@code awaitNanos}), interruptible waits, and several named conditions on one
 * lock if needed. Rules shown here:
 * <ul>
 *   <li>Always re-check the predicate in a {@code while} loop: spurious wake-ups happen, and another waiter may have
 *   taken the funds first.</li>
 *   <li>{@code signalAll}, not {@code signal}: waiters wait for DIFFERENT amounts; waking one that still can't proceed
 *   while another could would be a lost wake-up.</li>
 *   <li>Waiting never holds the lock (await releases it), so credits can arrive.</li>
 * </ul>
 * Production caveat: a thread blocked for minutes per payout does not scale, and is lost on restart. Real systems
 * persist the payout as PENDING_FUNDING and re-evaluate on the "funds received" event. This class shows the
 * in-process primitive.
 */
public final class FundingAwaitAccount {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition fundsArrived = lock.newCondition();
    private BigDecimal balance; // guarded by lock

    public FundingAwaitAccount(BigDecimal opening) {
        if (opening.signum() < 0) throw new IllegalArgumentException("opening must be >= 0");
        this.balance = opening;
    }

    public void credit(BigDecimal amount) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        lock.lock();
        try {
            balance = balance.add(amount);
            fundsArrived.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Debits {@code amount} as soon as it is available, waiting at most {@code timeout}.
     *
     * @return true if debited; false if the deadline passed (no partial debit ever happens)
     * @throws InterruptedException if the waiting thread is interrupted (e.g. service shutdown); state unchanged
     */
    public boolean debitWhenFunded(BigDecimal amount, Duration timeout) throws InterruptedException {
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        long remaining = timeout.toNanos();
        lock.lockInterruptibly();
        try {
            while (balance.compareTo(amount) < 0) {
                if (remaining <= 0) return false;
                remaining = fundsArrived.awaitNanos(remaining); // releases the lock while waiting
            }
            balance = balance.subtract(amount);
            return true;
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

    /** Diagnostic only (e.g. a gauge metric): number of threads currently waiting for funds. */
    public int waiters() {
        lock.lock();
        try {
            return lock.getWaitQueueLength(fundsArrived);
        } finally {
            lock.unlock();
        }
    }
}
