package module05.bounds;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) implements Comparable<Money> {
    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public Money plus(Money other) {
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("currency mismatch");
        return new Money(amount.add(other.amount), currency);
    }

    @Override
    public int compareTo(Money o) {
        if (!currency.equals(o.currency)) throw new IllegalArgumentException("currency mismatch");
        return amount.compareTo(o.amount);
    }
}
