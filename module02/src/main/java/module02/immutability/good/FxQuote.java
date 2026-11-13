package module02.immutability.good;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * GOOD: final class, final fields, immutable types (Instant, List.copyOf), no setters.
 * Changes produce new instances. Safe to publish across threads without synchronisation.
 */
public final class FxQuote {
    private final String pair;
    private final BigDecimal rate;
    private final Instant validUntil;
    private final List<String> venues;

    public FxQuote(String pair, BigDecimal rate, Instant validUntil, List<String> venues) {
        this.pair = Objects.requireNonNull(pair, "pair");
        this.rate = Objects.requireNonNull(rate, "rate");
        this.validUntil = Objects.requireNonNull(validUntil, "validUntil");
        this.venues = List.copyOf(Objects.requireNonNull(venues, "venues"));
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("rate must be positive");
        }
    }

    public FxQuote withRate(BigDecimal newRate) {
        return new FxQuote(pair, newRate, validUntil, venues);
    }

    public boolean isValidAt(Instant now) {
        return !now.isAfter(validUntil);
    }

    public String pair() { return pair; }
    public BigDecimal rate() { return rate; }
    public Instant validUntil() { return validUntil; }
    public List<String> venues() { return venues; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FxQuote)) return false;
        FxQuote q = (FxQuote) o;
        return pair.equals(q.pair) && rate.compareTo(q.rate) == 0
                && validUntil.equals(q.validUntil) && venues.equals(q.venues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pair, rate.stripTrailingZeros(), validUntil, venues);
    }
}
