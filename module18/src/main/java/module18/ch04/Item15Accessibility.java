package module18.ch04;

import java.util.List;

/**
 * Item 15 - Minimize the accessibility of classes and members.
 *
 * <p>Make every member as private as possible: the public API is a promise you must keep forever, while private
 * details can change. Watch out for {@code public static final} ARRAYS: the reference is final but the contents are
 * mutable, so any caller can rewrite the list of supported currencies. Expose an unmodifiable List or a copy.
 * Java 9 modules ({@code module-info.java} with selective {@code exports}) add a further layer.
 */
public final class Item15Accessibility {
    private Item15Accessibility() {
    }

    /** BAD: callers can do SUPPORTED_ARRAY[0] = "XXX". */
    public static final String[] SUPPORTED_ARRAY = {"EUR", "GBP", "USD"};

    /** GOOD: immutable view. */
    public static final List<String> SUPPORTED = List.of("EUR", "GBP", "USD");

    /** GOOD alternative: hand out a copy. */
    private static final String[] PRIVATE_SUPPORTED = {"EUR", "GBP", "USD"};

    public static String[] supportedCopy() {
        return PRIVATE_SUPPORTED.clone();
    }

    /** Package-private implementation detail: not part of the public API. */
    static int internalRoutingWeight(String currency) {
        return currency.hashCode() & 7;
    }
}
