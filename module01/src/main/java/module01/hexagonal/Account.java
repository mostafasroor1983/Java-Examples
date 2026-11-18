package module01.hexagonal;

import module01.immutability.Money;

import java.util.Objects;

/**
 * Domain entity with an optimistic-lock version. Immutable: debit/credit return a new instance with
 * version + 1, so a failed transaction can simply discard the new objects (no rollback code needed).
 */
public record Account(String id, Money balance, long version) {
    public Account {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(balance, "balance");
        if (balance.isNegative()) throw new IllegalArgumentException("balance cannot be negative for " + id);
        if (version < 0) throw new IllegalArgumentException("version must be >= 0");
    }

    public Account debit(Money amount) {
        Money after = balance.minus(amount); // throws on currency mismatch
        if (after.isNegative()) throw new InsufficientFundsException(id);
        return new Account(id, after, version + 1);
    }

    public Account credit(Money amount) {
        return new Account(id, balance.plus(amount), version + 1);
    }

    public static final class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String accountId) {
            super("insufficient funds on " + accountId); // no balance in the message: it ends up in logs
        }
    }
}
