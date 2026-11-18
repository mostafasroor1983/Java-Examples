package module18.ch02;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Item 1 - Consider static factory methods instead of constructors.
 *
 * <p>Named factories ({@code ofMinor}, {@code parse}, {@code zero}) say what they do, can return cached instances
 * ({@code zero} per currency), can validate and normalise before construction, and can return a subtype or a
 * different implementation later without breaking callers. The constructor is private.
 */
public final class Item01StaticFactories {
    private Item01StaticFactories() {
    }

    public static final class Money {
        private static final Map<Currency, Money> ZEROS = new ConcurrentHashMap<>();
        private final BigDecimal amount;
        private final Currency currency;

        private Money(BigDecimal amount, Currency currency) {
            this.currency = Objects.requireNonNull(currency);
            this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.UNNECESSARY);
        }

        /** Named: "100" means 100.00, never 1.00. */
        public static Money of(String amount, String currencyCode) {
            return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
        }

        /** Named alternative that a constructor could not express (same parameter types as another overload). */
        public static Money ofMinor(long minorUnits, String currencyCode) {
            Currency c = Currency.getInstance(currencyCode);
            return new Money(BigDecimal.valueOf(minorUnits, c.getDefaultFractionDigits()), c);
        }

        /** Parses "EUR 12.50". */
        public static Money parse(String text) {
            String[] parts = text.strip().split("\\s+");
            if (parts.length != 2) throw new IllegalArgumentException("expected 'CCY amount'");
            return of(parts[1], parts[0]);
        }

        /** Cached: every call returns the same instance per currency (instance control). */
        public static Money zero(String currencyCode) {
            Currency c = Currency.getInstance(currencyCode);
            return ZEROS.computeIfAbsent(c, k -> new Money(BigDecimal.ZERO, k));
        }

        public BigDecimal amount() {
            return amount;
        }

        public Currency currency() {
            return currency;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Money m && amount.equals(m.amount) && currency.equals(m.currency);
        }

        @Override
        public int hashCode() {
            return Objects.hash(amount, currency);
        }

        @Override
        public String toString() {
            return currency.getCurrencyCode() + " " + amount.toPlainString();
        }
    }
}
