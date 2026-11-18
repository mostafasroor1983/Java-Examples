package module18.ch06;

/**
 * Item 35 - Use instance fields instead of ordinals.
 *
 * <p>{@code ordinal()} is the declaration position, not business data. Deriving "settlement days" from it breaks the
 * day someone inserts {@code T_PLUS_0_INSTANT} at the top or reorders constants. Store the value in a field.
 * The only legitimate users of ordinal() are general-purpose structures like EnumSet and EnumMap.
 */
public final class Item35InstanceFieldsNotOrdinals {
    private Item35InstanceFieldsNotOrdinals() {
    }

    /** BAD: semantics depend on declaration order. */
    public enum SettlementCycleBad {
        T0, T1, T2;

        public int daysToSettle() {
            return ordinal();
        }
    }

    /** GOOD: explicit field; INSTANT added first without changing others' meaning. */
    public enum SettlementCycle {
        INSTANT(0), T0(0), T1(1), T2(2);

        private final int daysToSettle;

        SettlementCycle(int daysToSettle) {
            this.daysToSettle = daysToSettle;
        }

        public int daysToSettle() {
            return daysToSettle;
        }
    }
}
