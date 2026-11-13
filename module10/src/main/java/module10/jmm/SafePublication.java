package module10.jmm;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Four legal ways to publish an object to another thread:
 * 1. volatile field  2. AtomicReference  3. synchronized  4. final fields set in the constructor (for the object's own state).
 */
public final class SafePublication {
    public record FxRate(String pair, BigDecimal rate) {
    }

    private volatile FxRate viaVolatile;
    private final AtomicReference<FxRate> viaAtomic = new AtomicReference<>();
    private FxRate viaLock;
    private final Object lock = new Object();

    public void publishVolatile(FxRate r) {
        viaVolatile = r; // volatile write: everything before it is visible to a thread that reads it
    }

    public FxRate readVolatile() {
        return viaVolatile;
    }

    public void publishAtomic(FxRate r) {
        viaAtomic.set(r);
    }

    public FxRate readAtomic() {
        return viaAtomic.get();
    }

    public void publishLocked(FxRate r) {
        synchronized (lock) {
            viaLock = r;
        }
    }

    public FxRate readLocked() {
        synchronized (lock) {
            return viaLock;
        }
    }

    /** Final-field semantics: once the constructor completes, every thread sees `rate` correctly, even via a data race. */
    public static final class ImmutableQuote {
        private final BigDecimal rate;
        private final long[] history; // final reference; the array contents are also covered if written before construction completes

        public ImmutableQuote(BigDecimal rate, long[] history) {
            this.rate = rate;
            this.history = history.clone();
        }

        public BigDecimal rate() {
            return rate;
        }

        public long[] history() {
            return history.clone();
        }
    }
}
