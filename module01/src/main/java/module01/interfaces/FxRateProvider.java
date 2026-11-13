package module01.interfaces;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Interface = contract. Default methods give behaviour that is derived from the abstract ones,
 * static methods give factories; implementations stay tiny.
 */
public interface FxRateProvider {

    Optional<BigDecimal> rate(String base, String quote);

    default BigDecimal rateOrThrow(String base, String quote) {
        return rate(base, quote).orElseThrow(() ->
                new IllegalArgumentException("no rate for " + base + "/" + quote));
    }

    default Optional<BigDecimal> inverseRate(String base, String quote) {
        return rate(quote, base).map(r -> BigDecimal.ONE.divide(r, 10, java.math.RoundingMode.HALF_EVEN));
    }

    /** Try this provider first, then the fallback. */
    default FxRateProvider orElse(FxRateProvider fallback) {
        FxRateProvider primary = this;
        return (base, quote) -> primary.rate(base, quote).or(() -> fallback.rate(base, quote));
    }

    static FxRateProvider fixed(String base, String quote, BigDecimal value) {
        return (b, q) -> b.equals(base) && q.equals(quote) ? Optional.of(value) : Optional.empty();
    }
}
