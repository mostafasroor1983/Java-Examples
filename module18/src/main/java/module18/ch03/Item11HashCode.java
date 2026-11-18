package module18.ch03;

import java.util.Objects;

/**
 * Item 11 - Always override hashCode when you override equals.
 *
 * <p>Equal objects must have equal hash codes, or HashMap/HashSet/ConcurrentHashMap lookups fail and duplicates
 * appear (a deduplication set that lets the same payment through twice). Use the same fields as equals. For an
 * immutable class whose hash is expensive, the hash can be cached lazily (benign race: every thread computes the
 * same value), as String does.
 */
public final class Item11HashCode {
    private Item11HashCode() {
    }

    /** BAD: equals without hashCode. */
    public static final class BrokenAccountKey {
        private final String bankCode;
        private final String accountNumber;

        public BrokenAccountKey(String bankCode, String accountNumber) {
            this.bankCode = bankCode;
            this.accountNumber = accountNumber;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof BrokenAccountKey k && bankCode.equals(k.bankCode) && accountNumber.equals(k.accountNumber);
        }
        // hashCode missing -> identity hash
    }

    /** GOOD: consistent hashCode, lazily cached because the class is immutable. */
    public static final class AccountKey {
        private final String bankCode;
        private final String accountNumber;
        private int hash; // 0 = not yet computed

        public AccountKey(String bankCode, String accountNumber) {
            this.bankCode = Objects.requireNonNull(bankCode);
            this.accountNumber = Objects.requireNonNull(accountNumber);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof AccountKey k && bankCode.equals(k.bankCode) && accountNumber.equals(k.accountNumber);
        }

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                h = 31 * bankCode.hashCode() + accountNumber.hashCode();
                hash = h;
            }
            return h;
        }
    }
}
