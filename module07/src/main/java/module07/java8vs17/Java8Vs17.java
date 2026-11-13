package module07.java8vs17;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Side-by-side idioms. Each pair returns identical results so tests can prove equivalence. */
public final class Java8Vs17 {
    private Java8Vs17() {
    }

    // ---- immutable collections ----
    public static List<String> currenciesOld() {
        List<String> l = new ArrayList<>();
        l.add("GBP");
        l.add("EUR");
        return Collections.unmodifiableList(l);
    }

    public static List<String> currenciesNew() {
        return List.of("GBP", "EUR");
    }

    public static Map<String, Integer> decimalsOld() {
        Map<String, Integer> m = new HashMap<>();
        m.put("GBP", 2);
        m.put("JPY", 0);
        return Collections.unmodifiableMap(m);
    }

    public static Map<String, Integer> decimalsNew() {
        return Map.of("GBP", 2, "JPY", 0);
    }

    // ---- var ----
    public static int sumLengthsOld(List<String> ids) {
        int total = 0;
        for (String id : ids) total += id.length();
        return total;
    }

    public static int sumLengthsNew(List<String> ids) {
        var total = 0;
        for (var id : ids) total += id.length();
        return total;
    }

    // ---- Optional improvements ----
    public static String nameOrDefaultOld(Optional<String> name) {
        return name.isPresent() ? name.get() : "anonymous";
    }

    public static String nameOrDefaultNew(Optional<String> primary, Optional<String> secondary) {
        return primary.or(() -> secondary).orElse("anonymous");
    }

    // ---- Stream additions: takeWhile, dropWhile, toList, Collectors.teeing ----
    public static List<Integer> leadingSmallOld(List<Integer> sorted, int limit) {
        List<Integer> out = new ArrayList<>();
        for (int v : sorted) {
            if (v >= limit) break;
            out.add(v);
        }
        return out;
    }

    public static List<Integer> leadingSmallNew(List<Integer> sorted, int limit) {
        return sorted.stream().takeWhile(v -> v < limit).toList();
    }

    // ---- String methods ----
    public static List<String> nonBlankLinesOld(String text) {
        List<String> out = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            if (!line.trim().isEmpty()) out.add(line.trim());
        }
        return out;
    }

    public static List<String> nonBlankLinesNew(String text) {
        return text.lines().filter(l -> !l.isBlank()).map(String::strip).toList();
    }

    public static String repeatOld(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    public static String repeatNew(String s, int n) {
        return s.repeat(n);
    }

    // ---- data carrier ----
    public static final class PointOld {
        private final int x, y;

        public PointOld(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int x() { return x; }
        public int y() { return y; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PointOld)) return false;
            PointOld p = (PointOld) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return 31 * x + y;
        }

        @Override
        public String toString() {
            return "PointOld[x=" + x + ", y=" + y + "]";
        }
    }

    public record PointNew(int x, int y) {
    }

    public static Stream<String> ofNullableOld(String s) {
        return s == null ? Stream.empty() : Stream.of(s);
    }

    public static Stream<String> ofNullableNew(String s) {
        return Stream.ofNullable(s);
    }

    public static String joinOld(List<String> parts) {
        return parts.stream().collect(Collectors.joining(","));
    }

    public static String joinNew(List<String> parts) {
        return String.join(",", parts);
    }

    public static List<String> asListOld(String... items) {
        return Arrays.asList(items);
    }
}
