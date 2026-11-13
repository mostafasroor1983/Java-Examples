package module06.lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Streams are lazy: nothing runs until a terminal operation, and elements flow one at a time through
 * the whole pipeline (vertical processing). Short-circuiting ops (findFirst, limit, anyMatch) stop early.
 */
public final class LazyEvaluation {
    private LazyEvaluation() {
    }

    /** Records the order in which operations see elements. */
    public static List<String> trace(List<Integer> input) {
        List<String> log = new ArrayList<>();
        Stream<Integer> s = input.stream()
                .filter(i -> { log.add("filter " + i); return i % 2 == 0; })
                .map(i -> { log.add("map " + i); return i * 10; });
        log.add("pipeline built, nothing executed yet");
        s.findFirst().ifPresent(v -> log.add("found " + v));
        return log;
    }

    /** Infinite stream is fine as long as a short-circuit op bounds it. */
    public static List<Long> firstNFibonacci(int n) {
        return Stream.iterate(new long[]{0, 1}, f -> new long[]{f[1], f[0] + f[1]})
                .limit(n)
                .map(f -> f[0])
                .toList();
    }

    /** sorted() is a stateful barrier: it must see all elements before emitting any. */
    public static int elementsVisitedBeforeFirstAfterSort(List<Integer> input) {
        int[] visited = {0};
        input.stream().peek(i -> visited[0]++).sorted().findFirst();
        return visited[0];
    }

    /** A stream can be consumed once. */
    public static boolean canReuseStream(Stream<Integer> s) {
        s.count();
        try {
            s.count();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
