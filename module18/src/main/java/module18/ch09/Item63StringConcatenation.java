package module18.ch09;

import java.util.List;

/**
 * Item 63 - Beware the performance of string concatenation.
 *
 * <p>{@code +} in a loop copies the whole string each time: O(n^2) for n statement lines. Use a
 * {@link StringBuilder} (pre-sized when the size is known) or {@code String.join}/{@code Collectors.joining}.
 * A single expression {@code a + b + c} is fine: since Java 9 it compiles to an efficient {@code invokedynamic}
 * concatenation (JEP 280).
 */
public final class Item63StringConcatenation {
    private Item63StringConcatenation() {
    }

    public static String renderSlow(List<String> lines) {
        String out = "";
        for (String l : lines) out += l + "\n";
        return out;
    }

    public static String render(List<String> lines) {
        StringBuilder sb = new StringBuilder(lines.size() * 48);
        for (String l : lines) sb.append(l).append('\n');
        return sb.toString();
    }
}
