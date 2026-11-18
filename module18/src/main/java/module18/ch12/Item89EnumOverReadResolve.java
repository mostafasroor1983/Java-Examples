package module18.ch12;

import java.io.Serial;
import java.io.Serializable;

/**
 * Item 89 - For instance control, prefer enum types to readResolve.
 *
 * <p>Deserialization creates a NEW instance, which breaks a classic singleton. {@code readResolve} can substitute the
 * real instance, but every instance field must be transient or an attacker can smuggle a reference out before
 * readResolve runs. An enum singleton gets instance control from the JVM for free, under serialization and reflection.
 */
public final class Item89EnumOverReadResolve {
    private Item89EnumOverReadResolve() {
    }

    /** Fragile: relies on readResolve. */
    public static final class SuspenseAccount implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        public static final SuspenseAccount INSTANCE = new SuspenseAccount();

        private SuspenseAccount() {
        }

        @Serial
        private Object readResolve() {
            return INSTANCE; // discard the deserialized copy
        }
    }

    /** Preferred */
    public enum SystemAccount {
        SUSPENSE, FX_POSITION, FEES_INCOME
    }
}
