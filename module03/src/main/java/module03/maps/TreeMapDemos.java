package module03.maps;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** TreeMap = red-black tree: sorted keys, O(log n), navigation methods (floor/ceiling/subMap). */
public final class TreeMapDemos {
    private TreeMapDemos() {
    }

    /** Fee tiers: amount threshold -> fee rate. floorEntry picks the applicable tier. */
    public static final class FeeTiers {
        private final NavigableMap<BigDecimal, BigDecimal> tiers = new TreeMap<>();

        public FeeTiers tier(String fromAmount, String rate) {
            tiers.put(new BigDecimal(fromAmount), new BigDecimal(rate));
            return this;
        }

        public BigDecimal rateFor(BigDecimal amount) {
            Map.Entry<BigDecimal, BigDecimal> e = tiers.floorEntry(amount);
            if (e == null) {
                throw new IllegalArgumentException("no tier for amount " + amount);
            }
            return e.getValue();
        }
    }

    /** Daily balances; subMap gives a date range in O(log n + k). */
    public static final class BalanceHistory {
        private final TreeMap<LocalDate, BigDecimal> balances = new TreeMap<>();

        public void record(LocalDate day, BigDecimal balance) {
            balances.put(day, balance);
        }

        public NavigableMap<LocalDate, BigDecimal> between(LocalDate fromInclusive, LocalDate toInclusive) {
            return balances.subMap(fromInclusive, true, toInclusive, true);
        }

        /** Balance as of a date = latest known balance on or before it. */
        public BigDecimal asOf(LocalDate day) {
            Map.Entry<LocalDate, BigDecimal> e = balances.floorEntry(day);
            return e == null ? BigDecimal.ZERO : e.getValue();
        }
    }
}
