package module18.ch08;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * Item 50 - Make defensive copies when needed.
 *
 * <p>Assume callers will (accidentally or maliciously) mutate what they pass in or get back. A statement period
 * holding {@link Date}s can be altered after validation, so "start before end" no longer holds. Copy BEFORE validating
 * (otherwise another thread can change the value between check and copy: a TOCTOU window), do not use {@code clone()}
 * on untrusted subclasses, and copy on the way out. The real fix is immutable types: {@link Instant} (java.time).
 */
public final class Item50DefensiveCopies {
    private Item50DefensiveCopies() {
    }

    /** BAD: stores and returns the caller's mutable Dates. */
    public static final class LeakyPeriod {
        private final Date start;
        private final Date end;

        public LeakyPeriod(Date start, Date end) {
            if (start.after(end)) throw new IllegalArgumentException("start after end");
            this.start = start;
            this.end = end;
        }

        public Date start() { return start; }

        public Date end() { return end; }
    }

    /** GOOD with legacy types: copy in (before checking), copy out. */
    public static final class StatementPeriod {
        private final Date start;
        private final Date end;

        public StatementPeriod(Date start, Date end) {
            this.start = new Date(start.getTime());
            this.end = new Date(end.getTime());
            if (this.start.after(this.end)) throw new IllegalArgumentException("start after end");
        }

        public Date start() { return new Date(start.getTime()); }

        public Date end() { return new Date(end.getTime()); }
    }

    /** BEST: immutable components need no copies. */
    public record Period(Instant start, Instant end) {
        public Period {
            Objects.requireNonNull(start);
            Objects.requireNonNull(end);
            if (start.isAfter(end)) throw new IllegalArgumentException("start after end");
        }
    }
}
