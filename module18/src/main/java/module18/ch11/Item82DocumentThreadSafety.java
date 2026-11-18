package module18.ch11;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Item 82 - Document thread safety.
 *
 * <p>{@code synchronized} in a method signature is an implementation detail, not documentation. State the level
 * explicitly: immutable, unconditionally thread-safe, conditionally thread-safe (say which lock the caller must hold
 * for which operations), not thread-safe, or thread-hostile. Prefer a PRIVATE lock object over {@code this}, so
 * clients cannot (accidentally or maliciously) hold your lock and stall you. Annotations from {@link ThreadSafety}
 * make the contract visible.
 */
public final class Item82DocumentThreadSafety {
    private Item82DocumentThreadSafety() {
    }

    /** Unconditionally thread-safe: all state guarded by a private lock. */
    @ThreadSafety.ThreadSafe
    public static final class AccountStatement {
        private final Object lock = new Object();
        @ThreadSafety.GuardedBy("lock")
        private final List<BigDecimal> lines = new ArrayList<>();

        public void add(BigDecimal amount) {
            synchronized (lock) {
                lines.add(amount);
            }
        }

        public List<BigDecimal> snapshot() {
            synchronized (lock) {
                return List.copyOf(lines);
            }
        }
    }

    /** Not thread-safe: use from one thread, or synchronise externally. Cheaper for single-threaded batch jobs. */
    @ThreadSafety.NotThreadSafe
    public static final class BatchBuilder {
        private final List<String> refs = new ArrayList<>();

        public BatchBuilder add(String ref) {
            refs.add(ref);
            return this;
        }

        public List<String> build() {
            return List.copyOf(refs);
        }
    }

    @ThreadSafety.Immutable
    public record Rate(String pair, BigDecimal value) { }
}
