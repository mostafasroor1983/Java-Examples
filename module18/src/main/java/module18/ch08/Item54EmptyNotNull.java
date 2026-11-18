package module18.ch08;

import java.util.List;
import java.util.Map;

/**
 * Item 54 - Return empty collections or arrays, not nulls.
 *
 * <p>Returning null for "no transactions" forces every caller to null-check, and one forgotten check is an NPE in
 * production. Empty immutable collections ({@code List.of()}, {@code Collections.emptyList()}) cost nothing (shared
 * singletons). For arrays, return a zero-length array constant.
 */
public final class Item54EmptyNotNull {
    private static final String[] NO_TAGS = new String[0];
    private static final Map<String, List<String>> HISTORY = Map.of("ACC-1", List.of("T1", "T2"));

    private Item54EmptyNotNull() {
    }

    /** BAD */
    public static List<String> historyOrNull(String account) {
        return HISTORY.get(account);
    }

    /** GOOD */
    public static List<String> history(String account) {
        return HISTORY.getOrDefault(account, List.of());
    }

    public static String[] tags(String account) {
        return NO_TAGS;
    }
}
