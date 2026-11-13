package module15.p10aggregator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P10 - Concurrent aggregator: per-merchant count/sum/min/max, computed with a lock-free merge of immutable stats.
 */
public final class P10TransactionAggregator {
    public record Stats(long count, BigDecimal sum, BigDecimal min, BigDecimal max) {
        public static Stats of(BigDecimal v) {
            return new Stats(1, v, v, v);
        }

        public Stats merge(Stats o) {
            return new Stats(count + o.count, sum.add(o.sum), min.min(o.min), max.max(o.max));
        }

        public BigDecimal average() {
            return count == 0 ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_EVEN);
        }
    }

    private final ConcurrentHashMap<String, Stats> byMerchant = new ConcurrentHashMap<>();

    public void record(String merchantId, BigDecimal amount) {
        if (amount == null || amount.signum() < 0) throw new IllegalArgumentException("amount must be >= 0");
        byMerchant.merge(merchantId, Stats.of(amount), Stats::merge);
    }

    public Stats stats(String merchantId) {
        return byMerchant.getOrDefault(merchantId, new Stats(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    public Map<String, Stats> snapshot() {
        return new TreeMap<>(byMerchant);
    }

    public Stats overall() {
        return byMerchant.values().stream().reduce(Stats::merge).orElse(stats("none"));
    }
}
