package com.revolut.interview.bank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/** Minimal Money value object for the katas: scale-normalised, currency-checked arithmetic. */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {
    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currency));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money o) {
        check(o);
        return new Money(amount.add(o.amount), currency);
    }

    public Money minus(Money o) {
        check(o);
        return new Money(amount.subtract(o.amount), currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    private void check(Money o) {
        if (!currency.equals(o.currency)) throw new CurrencyMismatchException(currency, o.currency);
    }

    @Override
    public int compareTo(Money o) {
        check(o);
        return amount.compareTo(o.amount);
    }

    public static final class CurrencyMismatchException extends IllegalArgumentException {
        public CurrencyMismatchException(Currency a, Currency b) {
            super("currency mismatch: " + a + " vs " + b);
        }
    }
}
