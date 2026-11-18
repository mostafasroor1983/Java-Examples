package module18.ch02;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Item 2 - Consider a builder when faced with many constructor parameters.
 *
 * <p>A payment instruction has 3 required and several optional parameters. Telescoping constructors are unreadable
 * ({@code new Payment("A", "B", amt, null, null, true, false)}) and JavaBeans setters leave the object mutable and
 * possibly half-built. The builder takes required values up front, optional ones by name, and validates
 * cross-field rules once in {@link Builder#build()}. The product is an immutable record (Java 16+).
 */
public final class Item02Builder {
    private Item02Builder() {
    }

    public enum Priority { NORMAL, URGENT }

    public record PaymentInstruction(String debtorIban, String creditorIban, BigDecimal amount, String reference,
                                     LocalDate executionDate, Priority priority, boolean instant) {
        public static Builder builder(String debtorIban, String creditorIban, BigDecimal amount) {
            return new Builder(debtorIban, creditorIban, amount);
        }

        public Optional<String> referenceOpt() {
            return Optional.ofNullable(reference);
        }
    }

    public static final class Builder {
        private final String debtorIban;
        private final String creditorIban;
        private final BigDecimal amount;
        private String reference;
        private LocalDate executionDate; // null = execute as soon as possible
        private Priority priority = Priority.NORMAL;
        private boolean instant;

        private Builder(String debtorIban, String creditorIban, BigDecimal amount) {
            this.debtorIban = Objects.requireNonNull(debtorIban);
            this.creditorIban = Objects.requireNonNull(creditorIban);
            this.amount = Objects.requireNonNull(amount);
        }

        public Builder reference(String value) {
            this.reference = value;
            return this;
        }

        public Builder executionDate(LocalDate value) {
            this.executionDate = Objects.requireNonNull(value);
            return this;
        }

        public Builder priority(Priority value) {
            this.priority = Objects.requireNonNull(value);
            return this;
        }

        public Builder instant(boolean value) {
            this.instant = value;
            return this;
        }

        /** Cross-field invariants are checked once, here. */
        public PaymentInstruction build() {
            if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
            if (debtorIban.equals(creditorIban)) throw new IllegalArgumentException("debtor and creditor must differ");
            if (instant && amount.compareTo(new BigDecimal("100000")) > 0) {
                throw new IllegalArgumentException("instant payments are limited to 100000");
            }
            if (reference != null && reference.length() > 140) throw new IllegalArgumentException("reference too long");
            return new PaymentInstruction(debtorIban, creditorIban, amount, reference, executionDate, priority, instant);
        }
    }
}
