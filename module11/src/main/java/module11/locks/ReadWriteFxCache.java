package module11.locks;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ReentrantReadWriteLock: many concurrent readers, exclusive writers.
 * Good when reads vastly outnumber writes (FX rate cache read on every payment, refreshed every few seconds).
 * Caveat: write-starvation under constant reads unless fair; downgrade write->read is allowed, upgrade is not.
 */
public final class ReadWriteFxCache {
    private final ReadWriteLock rw = new ReentrantReadWriteLock();
    private final Map<String, BigDecimal> rates = new HashMap<>();
    private long reads, writes;

    public Optional<BigDecimal> rate(String pair) {
        rw.readLock().lock();
        try {
            return Optional.ofNullable(rates.get(pair));
        } finally {
            rw.readLock().unlock();
        }
    }

    public void refresh(Map<String, BigDecimal> fresh) {
        rw.writeLock().lock();
        try {
            rates.clear();
            rates.putAll(fresh);
            writes++;
        } finally {
            rw.writeLock().unlock();
        }
    }

    /** Lock downgrade: hold write, acquire read, release write - readers see the new snapshot atomically. */
    public BigDecimal refreshAndRead(Map<String, BigDecimal> fresh, String pair) {
        rw.writeLock().lock();
        try {
            rates.clear();
            rates.putAll(fresh);
            writes++;
            rw.readLock().lock(); // downgrade
        } finally {
            rw.writeLock().unlock();
        }
        try {
            return rates.get(pair);
        } finally {
            rw.readLock().unlock();
        }
    }

    public int size() {
        rw.readLock().lock();
        try {
            return rates.size();
        } finally {
            rw.readLock().unlock();
        }
    }
}
