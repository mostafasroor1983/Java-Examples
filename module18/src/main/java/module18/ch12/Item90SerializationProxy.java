package module18.ch12;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Item 90 - Prefer serialization proxies to readObject.
 *
 * <p>The outer class stays fully immutable (final fields) and its only "constructor" is the public one. A private
 * static nested proxy captures the logical state; {@code writeReplace} serializes the proxy instead of the object; the
 * proxy's {@code readResolve} rebuilds the object THROUGH the public constructor/factory, so all invariants are
 * enforced; {@code readObject} on the outer class throws, so a forged stream cannot construct it directly.
 * Records give most of this for free (Java 16+), which is why they are the modern default for serializable values.
 */
public final class Item90SerializationProxy {
    private Item90SerializationProxy() {
    }

    /** SEPA direct-debit mandate. */
    public static final class Mandate implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L; // Item 86: always explicit

        private final String mandateId;
        private final String debtorIban;
        private final BigDecimal maxAmount;

        public Mandate(String mandateId, String debtorIban, BigDecimal maxAmount) {
            this.mandateId = Objects.requireNonNull(mandateId);
            this.debtorIban = Objects.requireNonNull(debtorIban);
            this.maxAmount = Objects.requireNonNull(maxAmount);
            if (maxAmount.signum() <= 0) throw new IllegalArgumentException("max amount must be positive");
            if (!mandateId.matches("[A-Za-z0-9+?/:().,' -]{1,35}")) throw new IllegalArgumentException("invalid mandate id");
        }

        public String mandateId() { return mandateId; }

        public String debtorIban() { return debtorIban; }

        public BigDecimal maxAmount() { return maxAmount; }

        @Serial
        private Object writeReplace() {
            return new Proxy(this);
        }

        @Serial
        private void readObject(ObjectInputStream in) throws InvalidObjectException {
            throw new InvalidObjectException("proxy required");
        }

        private static final class Proxy implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;
            private final String mandateId;
            private final String debtorIban;
            private final BigDecimal maxAmount;

            Proxy(Mandate m) {
                this.mandateId = m.mandateId;
                this.debtorIban = m.debtorIban;
                this.maxAmount = m.maxAmount;
            }

            @Serial
            private Object readResolve() {
                return new Mandate(mandateId, debtorIban, maxAmount); // invariants re-checked
            }
        }
    }
}
