package module01.exercise1;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Exercise 1: a Wallet that encapsulates its balance, enforces non-negativity, rejects
 * cross-currency operations and records every movement. Thread-safe via intrinsic lock.
 */
public final class Wallet {
    public enum Direction { CREDIT, DEBIT }

    public record Movement(Direction direction, BigDecimal amount, String reference) {
    }

    private final WalletId id;
    private final Currency currency;
    private BigDecimal balance;
    private final List<Movement> movements = new ArrayList<>();

    public Wallet(WalletId id, Currency currency) {
        this.id = Objects.requireNonNull(id, "id");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.balance = BigDecimal.ZERO.setScale(currency.getDefaultFractionDigits());
    }

    public synchronized void credit(BigDecimal amount, Currency amountCurrency, String reference) {
        validate(amount, amountCurrency, reference);
        balance = balance.add(amount);
        movements.add(new Movement(Direction.CREDIT, amount, reference));
    }

    public synchronized void debit(BigDecimal amount, Currency amountCurrency, String reference) {
        validate(amount, amountCurrency, reference);
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient funds in wallet " + id.value()
                    + ": balance=" + balance + ", requested=" + amount);
        }
        balance = balance.subtract(amount);
        movements.add(new Movement(Direction.DEBIT, amount, reference));
    }

    public synchronized BigDecimal balance() {
        return balance;
    }

    public synchronized List<Movement> movements() {
        return List.copyOf(movements);
    }

    public WalletId id() {
        return id;
    }

    public Currency currency() {
        return currency;
    }

    private void validate(BigDecimal amount, Currency amountCurrency, String reference) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (!currency.equals(amountCurrency)) {
            throw new IllegalArgumentException("currency mismatch: wallet is " + currency + ", got " + amountCurrency);
        }
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
    }
}
