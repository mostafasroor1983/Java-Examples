package module18.ch06;

import java.util.EnumSet;
import java.util.Set;

/**
 * Item 36 - Use EnumSet instead of bit fields.
 *
 * <p>Bit fields ({@code READ | APPROVE}) are unreadable when printed, have a fixed maximum width and no type safety.
 * {@link EnumSet} is internally a bit vector (as fast), but type-safe, readable ({@code [VIEW, APPROVE]}) and a real
 * {@link Set}. Accept {@code Set<Permission>} in APIs; use EnumSet as the implementation.
 */
public final class Item36EnumSet {
    private Item36EnumSet() {
    }

    public enum Permission { VIEW_BALANCES, INITIATE_PAYMENT, APPROVE_PAYMENT, MANAGE_LIMITS, EXPORT_DATA }

    /** BAD style kept for comparison. */
    public static final int VIEW = 1, INITIATE = 1 << 1, APPROVE = 1 << 2;

    public static Set<Permission> operatorRole() {
        return EnumSet.of(Permission.VIEW_BALANCES, Permission.INITIATE_PAYMENT);
    }

    public static Set<Permission> supervisorRole() {
        return EnumSet.complementOf(EnumSet.of(Permission.EXPORT_DATA));
    }

    /** Separation of duties: nobody may hold both INITIATE and APPROVE. */
    public static boolean violatesSeparationOfDuties(Set<Permission> granted) {
        return granted.containsAll(EnumSet.of(Permission.INITIATE_PAYMENT, Permission.APPROVE_PAYMENT));
    }
}
