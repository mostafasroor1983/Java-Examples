package module10.lockfree;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Lock-free available-credit tracking for card authorisations, in minor units (cents) as a {@code long}.
 *
 * <p>Invariant: {@code 0 <= reserved <= limit} at every instant, under any number of threads. Authorisation is a
 * conditional update ("reserve only if it fits"), which a plain {@code addAndGet} cannot express: it would overshoot and
 * then need compensating, leaving a window where the limit is exceeded. Hence the CAS loop.
 *
 * <p>Uses a {@link VarHandle} (Java 9+) instead of AtomicLong to show the memory-ordering modes explicitly:
 * <ul>
 *   <li>{@code getAcquire}: no later read/write in this thread can move before it.</li>
 *   <li>{@code compareAndSet}: full volatile semantics. On success, the update is visible to every later reader.</li>
 * </ul>
 * In application code, prefer {@code AtomicLong}; VarHandles are for library or infrastructure code and for
 * avoiding one object per field in very large arrays of counters.
 *
 * <p>Scope: one JVM. A card processor running N authorisation nodes needs the limit in one authoritative place
 * (a partition owner per card or a DB conditional update); N in-memory counters would each allow the full limit.
 */
public final class CreditLimitReserver {
    private static final VarHandle RESERVED;

    static {
        try {
            RESERVED = MethodHandles.lookup().findVarHandle(CreditLimitReserver.class, "reserved", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final long limitMinor;
    @SuppressWarnings("unused") // accessed via VarHandle
    private volatile long reserved;

    public CreditLimitReserver(long limitMinor) {
        if (limitMinor < 0) throw new IllegalArgumentException("limit must be >= 0");
        this.limitMinor = limitMinor;
    }

    /** @return true if the amount was reserved; false (no state change) if it would exceed the limit. */
    public boolean tryReserve(long amountMinor) {
        if (amountMinor <= 0) throw new IllegalArgumentException("amount must be positive");
        while (true) {
            long current = (long) RESERVED.getAcquire(this);
            long next = current + amountMinor;
            if (next > limitMinor || next < 0) return false;  // also guards long overflow
            if (RESERVED.compareAndSet(this, current, next)) return true;
            Thread.onSpinWait(); // hint to the CPU while contending
        }
    }

    /** Releases a previous reservation (authorisation reversed or expired). Never goes below zero. */
    public void release(long amountMinor) {
        if (amountMinor <= 0) throw new IllegalArgumentException("amount must be positive");
        while (true) {
            long current = (long) RESERVED.getAcquire(this);
            if (amountMinor > current) throw new IllegalStateException("release exceeds reserved: corrupt bookkeeping");
            if (RESERVED.compareAndSet(this, current, current - amountMinor)) return;
            Thread.onSpinWait();
        }
    }

    public long reserved() {
        return (long) RESERVED.getVolatile(this);
    }

    public long available() {
        return limitMinor - reserved();
    }
}
