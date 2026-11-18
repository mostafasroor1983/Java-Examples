package module18.ch04;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Item 17 - Minimize mutability.
 *
 * <p>Immutable classes are simple (one state forever), thread-safe without synchronisation, freely shareable and
 * cacheable, and make great map keys. Recipe: no mutators, final class, private final fields, defensive copies of
 * mutable components, and "wither" methods that return new instances. Common values can be cached as constants.
 */
public final class Item17Immutability {
    private Item17Immutability() {
    }

    /** Annual interest rate in percent, e.g. 3.25. */
    public static final class InterestRate {
        public static final InterestRate ZERO = new InterestRate(BigDecimal.ZERO);
        private final BigDecimal percent;

        private InterestRate(BigDecimal percent) {
            this.percent = percent.setScale(4, RoundingMode.HALF_EVEN);
        }

        public static InterestRate ofPercent(String percent) {
            BigDecimal p = new BigDecimal(percent);
            if (p.signum() == 0) return ZERO;
            if (p.compareTo(new BigDecimal("-10")) < 0 || p.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("rate out of range");
            }
            return new InterestRate(p);
        }

        /** Returns a NEW instance; this one never changes. */
        public InterestRate plusBasisPoints(int bps) {
            return new InterestRate(percent.add(BigDecimal.valueOf(bps, 2)));
        }

        /** Simple daily interest on a balance (ACT/365). */
        public BigDecimal dailyInterest(BigDecimal balance) {
            return balance.multiply(percent).divide(BigDecimal.valueOf(36_500), 2, RoundingMode.HALF_EVEN);
        }

        public BigDecimal percent() {
            return percent;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof InterestRate r && percent.equals(r.percent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(percent);
        }
    }
}
