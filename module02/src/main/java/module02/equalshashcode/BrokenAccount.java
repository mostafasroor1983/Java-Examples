package module02.equalshashcode;

import java.util.Objects;

/**
 * BAD: equals() overridden, hashCode() not. Two "equal" accounts land in different HashMap buckets,
 * so HashSet.contains() and HashMap.get() lie.
 */
public class BrokenAccount {
    private final String iban;

    public BrokenAccount(String iban) {
        this.iban = Objects.requireNonNull(iban);
    }

    public String iban() {
        return iban;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BrokenAccount)) return false;
        return iban.equals(((BrokenAccount) o).iban);
    }
    // hashCode() deliberately missing - inherits identity hash
}
