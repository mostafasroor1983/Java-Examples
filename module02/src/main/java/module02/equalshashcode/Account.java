package module02.equalshashcode;

import java.util.Objects;

/**
 * GOOD: final class, getClass()-free instanceof is fine because the class cannot be subclassed,
 * equals and hashCode use the same fields, and both are consistent with the business identity (IBAN).
 */
public final class Account {
    private final String iban;
    private final String holderName; // not part of identity

    public Account(String iban, String holderName) {
        this.iban = Objects.requireNonNull(iban, "iban");
        this.holderName = Objects.requireNonNull(holderName, "holderName");
    }

    public String iban() {
        return iban;
    }

    public String holderName() {
        return holderName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return iban.equals(account.iban);
    }

    @Override
    public int hashCode() {
        return iban.hashCode();
    }

    @Override
    public String toString() {
        return "Account[" + iban + "]";
    }
}
