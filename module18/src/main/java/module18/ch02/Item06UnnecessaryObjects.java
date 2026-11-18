package module18.ch02;

import java.util.regex.Pattern;

/**
 * Item 6 - Avoid creating unnecessary objects.
 *
 * <p>{@code String.matches} compiles a regex {@link Pattern} on EVERY call; validating millions of IBANs per day that
 * way wastes CPU and allocation. Compile once into a static final constant. Similarly, {@code Long sum} in a loop boxes
 * on each iteration. But do not pool cheap objects "for speed": modern GCs make short-lived objects nearly free, and
 * pools add bugs. Defensive copies (Item 50) are NOT unnecessary objects.
 */
public final class Item06UnnecessaryObjects {
    private static final Pattern IBAN_SHAPE = Pattern.compile("[A-Z]{2}\\d{2}[A-Z0-9]{11,30}");

    private Item06UnnecessaryObjects() {
    }

    /** BAD: compiles the pattern every time. */
    public static boolean looksLikeIbanSlow(String s) {
        return s.matches("[A-Z]{2}\\d{2}[A-Z0-9]{11,30}");
    }

    /** GOOD: reuses the precompiled, immutable, thread-safe Pattern. */
    public static boolean looksLikeIban(String s) {
        return IBAN_SHAPE.matcher(s).matches();
    }

    /** BAD: Long accumulator boxes on every +=. */
    public static long sumBoxed(long[] cents) {
        Long sum = 0L;
        for (long c : cents) sum += c;
        return sum;
    }

    public static long sum(long[] cents) {
        long sum = 0;
        for (long c : cents) sum += c;
        return sum;
    }
}
