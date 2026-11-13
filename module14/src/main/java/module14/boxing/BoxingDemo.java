package module14.boxing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Autoboxing hides allocation and identity surprises. Integer cache is -128..127; Long the same; Double/Float none.
 * In hot loops use primitives, primitive streams, and primitive-specialised collections if you have them.
 */
public final class BoxingDemo {
    private BoxingDemo() {
    }

    /** BAD: Long sum boxes/unboxes on every iteration. */
    public static Long sumBoxed(int n) {
        Long sum = 0L;
        for (int i = 0; i < n; i++) sum += i;
        return sum;
    }

    public static long sumPrimitive(int n) {
        long sum = 0;
        for (int i = 0; i < n; i++) sum += i;
        return sum;
    }

    /** BAD: Stream<Integer> boxes; GOOD: IntStream does not. */
    public static int sumStreamBoxed(int n) {
        return IntStream.range(0, n).boxed().mapToInt(Integer::intValue).sum();
    }

    public static int sumStreamPrimitive(int n) {
        return IntStream.range(0, n).sum();
    }

    /** Identity traps. */
    public static boolean integerCacheHit(int v) {
        Integer a = v, b = v;
        return a == b;
    }

    public static boolean longEqualsInt() {
        Long l = 1L;
        Integer i = 1;
        return l.equals(i); // false: different classes
    }

    /** NPE trap: unboxing null. */
    public static int unboxNull(Map<String, Integer> m, String key) {
        return m.get(key); // NullPointerException when key is absent
    }

    public static int unboxNullSafe(Map<String, Integer> m, String key) {
        return m.getOrDefault(key, 0);
    }

    /** List.remove(int) vs remove(Object): overload resolution with boxing. */
    public static List<Integer> removeTrap() {
        List<Integer> list = new ArrayList<>(List.of(10, 20, 30));
        list.remove(1);                     // removes index 1 (20)
        list.remove(Integer.valueOf(10));   // removes the element 10
        return list;                        // [30]
    }

    /** Counting with a boxed map vs a primitive array when the key space is small. */
    public static int[] countStatusesPrimitive(int[] statuses) {
        int[] counts = new int[6];
        for (int s : statuses) counts[Math.min(5, s / 100)]++;
        return counts;
    }

    public static Map<Integer, Integer> countStatusesBoxed(int[] statuses) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int s : statuses) counts.merge(s / 100, 1, Integer::sum);
        return counts;
    }
}
