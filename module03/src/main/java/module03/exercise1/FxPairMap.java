package module03.exercise1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Exercise 1: FX pair lookup. Key is a record (base, quote). Provides direct, inverse and
 * cross-rate-through-USD lookups. Shows why a composite record key beats string concatenation.
 */
public final class FxPairMap {
    public record Pair(String base, String quote) {
        public Pair {
            Objects.requireNonNull(base);
            Objects.requireNonNull(quote);
            if (base.length() != 3 || quote.length() != 3) {
                throw new IllegalArgumentException("currency codes must have 3 letters");
            }
            if (base.equals(quote)) {
                throw new IllegalArgumentException("base and quote must differ");
            }
        }

        public Pair inverse() {
            return new Pair(quote, base);
        }
    }

    private static final int SCALE = 8;
    private final Map<Pair, BigDecimal> rates = new HashMap<>();

    public void put(String base, String quote, BigDecimal rate) {
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("rate must be positive");
        }
        rates.put(new Pair(base, quote), rate);
    }

    public Optional<BigDecimal> direct(String base, String quote) {
        return Optional.ofNullable(rates.get(new Pair(base, quote)));
    }

    /** direct, then inverse (1/r), then cross via USD. */
    public Optional<BigDecimal> rate(String base, String quote) {
        if (base.equals(quote)) return Optional.of(BigDecimal.ONE);
        Optional<BigDecimal> d = direct(base, quote);
        if (d.isPresent()) return d;
        Optional<BigDecimal> inv = direct(quote, base).map(r -> BigDecimal.ONE.divide(r, SCALE, RoundingMode.HALF_EVEN));
        if (inv.isPresent()) return inv;
        if (!"USD".equals(base) && !"USD".equals(quote)) {
            Optional<BigDecimal> leg1 = rate(base, "USD");
            Optional<BigDecimal> leg2 = rate("USD", quote);
            if (leg1.isPresent() && leg2.isPresent()) {
                return Optional.of(leg1.get().multiply(leg2.get()).setScale(SCALE, RoundingMode.HALF_EVEN));
            }
        }
        return Optional.empty();
    }

    public int size() {
        return rates.size();
    }
}
