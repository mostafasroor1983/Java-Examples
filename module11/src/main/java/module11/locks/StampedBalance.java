package module11.locks;

import java.math.BigDecimal;
import java.util.concurrent.locks.StampedLock;

/**
 * StampedLock: optimistic reads cost no lock acquisition. Read, validate the stamp, fall back to a read lock
 * if a write happened in between. Not reentrant; no Conditions; never hold it across blocking calls.
 */
public final class StampedBalance {
    private final StampedLock sl = new StampedLock();
    private BigDecimal available;
    private BigDecimal reserved;
    private long optimisticFailures;

    public StampedBalance(BigDecimal available) {
        this.available = available;
        this.reserved = BigDecimal.ZERO;
    }

    /** Optimistic read of two fields that must be consistent with each other. */
    public BigDecimal total() {
        long stamp = sl.tryOptimisticRead();
        BigDecimal a = available, r = reserved;
        if (!sl.validate(stamp)) {
            stamp = sl.readLock();
            try {
                optimisticFailures++;
                a = available;
                r = reserved;
            } finally {
                sl.unlockRead(stamp);
            }
        }
        return a.add(r);
    }

    public boolean reserve(BigDecimal amount) {
        long stamp = sl.writeLock();
        try {
            if (available.compareTo(amount) < 0) return false;
            available = available.subtract(amount);
            reserved = reserved.add(amount);
            return true;
        } finally {
            sl.unlockWrite(stamp);
        }
    }

    public void release(BigDecimal amount) {
        long stamp = sl.writeLock();
        try {
            reserved = reserved.subtract(amount);
            available = available.add(amount);
        } finally {
            sl.unlockWrite(stamp);
        }
    }

    /** Conditional write via tryConvertToWriteLock: read, decide, upgrade without releasing. */
    public boolean capture(BigDecimal amount) {
        long stamp = sl.readLock();
        try {
            while (true) {
                if (reserved.compareTo(amount) < 0) return false;
                long ws = sl.tryConvertToWriteLock(stamp);
                if (ws != 0L) {
                    stamp = ws;
                    reserved = reserved.subtract(amount); // money leaves the account
                    return true;
                }
                sl.unlockRead(stamp);
                stamp = sl.writeLock();
            }
        } finally {
            sl.unlock(stamp);
        }
    }

    public BigDecimal available() {
        long stamp = sl.readLock();
        try {
            return available;
        } finally {
            sl.unlockRead(stamp);
        }
    }

    public BigDecimal reserved() {
        long stamp = sl.readLock();
        try {
            return reserved;
        } finally {
            sl.unlockRead(stamp);
        }
    }
}
