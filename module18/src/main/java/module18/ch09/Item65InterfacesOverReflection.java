package module18.ch09;

import java.math.BigDecimal;

/**
 * Item 65 - Prefer interfaces to reflection.
 *
 * <p>Reflection loses compile-time checking, is verbose and slower. When a class is only known at runtime (plugin,
 * configured fee policy), use reflection ONLY to instantiate it, then use it through a compile-time interface.
 * Translate the many reflective exceptions into one meaningful exception. ServiceLoader is often a better option.
 */
public final class Item65InterfacesOverReflection {
    private Item65InterfacesOverReflection() {
    }

    public interface FeePolicy {
        BigDecimal fee(BigDecimal amount);
    }

    public static final class FlatOneEuro implements FeePolicy {
        public BigDecimal fee(BigDecimal amount) {
            return BigDecimal.ONE;
        }
    }

    /** Instantiate reflectively, use via the interface. */
    public static FeePolicy load(String className) {
        try {
            Class<? extends FeePolicy> type = Class.forName(className).asSubclass(FeePolicy.class);
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalArgumentException("cannot load fee policy " + className, e);
        }
    }
}
