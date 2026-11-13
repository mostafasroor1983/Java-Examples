package module06.parallel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Parallel streams run on the common ForkJoinPool. Rules:
 * - stateless, non-interfering, associative operations only
 * - never mutate shared state from the lambda (use collect, not forEach-into-list)
 * - good for large, CPU-bound, splittable sources (ArrayList, arrays, ranges); bad for I/O, small data, LinkedList
 */
public final class ParallelStreams {
    private ParallelStreams() {
    }

    public static long sumRange(long n) {
        return LongStream.rangeClosed(1, n).parallel().sum();
    }

    /** BAD: shared ArrayList mutated from parallel forEach -> lost elements or AIOOBE. */
    public static List<Integer> collectBad(int n) {
        List<Integer> out = new ArrayList<>();
        java.util.stream.IntStream.range(0, n).parallel().forEach(out::add);
        return out;
    }

    /** GOOD: collect() merges thread-local containers. */
    public static List<Integer> collectGood(int n) {
        return java.util.stream.IntStream.range(0, n).parallel().boxed().collect(Collectors.toList());
    }

    /** reduce needs an identity and an associative op; BigDecimal.add qualifies. */
    public static BigDecimal totalParallel(List<BigDecimal> amounts) {
        return amounts.parallelStream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** BAD reduce: subtraction is not associative; parallel result differs from sequential. */
    public static long nonAssociativeReduce(List<Long> values, boolean parallel) {
        var s = parallel ? values.parallelStream() : values.stream();
        return s.reduce(0L, (a, b) -> a - b);
    }

    /** Running on a dedicated pool to avoid starving the common pool (the classic trick). */
    public static long sumOnDedicatedPool(long n, int parallelism) throws Exception {
        ForkJoinPool pool = new ForkJoinPool(parallelism);
        try {
            return pool.submit(() -> LongStream.rangeClosed(1, n).parallel().sum()).get();
        } finally {
            pool.shutdown();
        }
    }

    /** Ordered output is preserved with forEachOrdered / collect, not with forEach. */
    public static List<Integer> orderedParallel(int n) {
        return java.util.stream.IntStream.range(0, n).parallel().boxed().collect(Collectors.toList());
    }
}
