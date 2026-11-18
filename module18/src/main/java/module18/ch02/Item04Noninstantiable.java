package module18.ch02;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Item 4 - Enforce noninstantiability with a private constructor.
 *
 * <p>Utility classes (static helpers only) should not be instantiable or subclassable. Making the class abstract does
 * not work (a subclass can be instantiated). A private constructor that throws {@link AssertionError} documents the
 * intent and also blocks reflective instantiation from inside the class.
 */
public final class Item04Noninstantiable {
    private Item04Noninstantiable() {
        throw new AssertionError("utility class - do not instantiate");
    }

    /** Basis points (1 bp = 0.01%) applied to an amount, rounded HALF_EVEN to 2 decimals. */
    public static BigDecimal applyBps(BigDecimal amount, int bps) {
        return amount.multiply(BigDecimal.valueOf(bps)).divide(BigDecimal.valueOf(10_000), 2, RoundingMode.HALF_EVEN);
    }
}
