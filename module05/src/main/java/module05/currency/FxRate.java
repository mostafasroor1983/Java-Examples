package module05.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A directed FX rate: 1 unit of F = rate units of T. {@code convert} only accepts {@code Amount<F>} and returns
 * {@code Amount<T>}, so applying a EUR->USD rate to a GBP amount, or the inverse direction, does not compile.
 * The result is rounded HALF_EVEN to the target currency's scale exactly once, at the end.
 */
public record FxRate<F extends CurrencyUnit, T extends CurrencyUnit>(F from, T to, BigDecimal rate) {
    public FxRate {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(rate, "rate");
        if (rate.signum() <= 0) throw new IllegalArgumentException("rate must be positive");
        if (from.equals(to)) throw new IllegalArgumentException("same-currency rate");
    }

    public Amount<T> convert(Amount<F> amount) {
        return new Amount<>(amount.value().multiply(rate).setScale(to.scale(), RoundingMode.HALF_EVEN), to);
    }

    /** The inverse rate, computed with enough precision (10 dp) to avoid round-trip drift on large amounts. */
    public FxRate<T, F> inverse() {
        return new FxRate<>(to, from, BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_EVEN));
    }

    /** Cross rate: (F->T) then (T->X) gives F->X; the middle currency must line up, which the compiler checks. */
    public <X extends CurrencyUnit> FxRate<F, X> then(FxRate<T, X> next) {
        return new FxRate<>(from, next.to(), rate.multiply(next.rate()));
    }
}
