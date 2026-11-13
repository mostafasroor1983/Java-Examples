package module01.immutability;

import java.time.Instant;
import java.util.Objects;

/** Immutable record with validation in the compact constructor. Safe to share across threads. */
public record Transfer(String id, String fromAccount, String toAccount, Money amount, Instant createdAt) {
    public Transfer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fromAccount, "fromAccount");
        Objects.requireNonNull(toAccount, "toAccount");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(createdAt, "createdAt");
        if (fromAccount.equals(toAccount)) {
            throw new IllegalArgumentException("cannot transfer to the same account");
        }
        if (amount.isNegative() || amount.amount().signum() == 0) {
            throw new IllegalArgumentException("transfer amount must be positive");
        }
    }

    /** "Wither": change = new instance. */
    public Transfer withAmount(Money newAmount) {
        return new Transfer(id, fromAccount, toAccount, newAmount, createdAt);
    }
}
