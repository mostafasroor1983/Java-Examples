package module18.ch07;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

/**
 * Item 48 - Use caution when making streams parallel.
 *
 * <p>{@code .parallel()} helps only for large, CPU-bound work over sources that split well (arrays, ArrayList,
 * {@code IntStream.range}) with associative, side-effect-free reductions. It does NOT help for I/O (it uses the shared
 * common ForkJoinPool), for {@code Stream.iterate}/{@code limit}, or for small data. Side effects into a non-thread-safe
 * collection silently lose data. Always measure; correctness first.
 */
public final class Item48ParallelStreams {
    private Item48ParallelStreams() {
    }

    /** CORRECT in parallel: associative reduction with an identity. */
    public static BigDecimal totalParallel(List<BigDecimal> amounts) {
        return amounts.parallelStream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** BROKEN in parallel: side effect into ArrayList (may lose elements or throw). Do not do this. */
    public static int collectWithSideEffect(int n) {
        List<Long> out = new ArrayList<>();
        try {
            LongStream.range(0, n).parallel().forEach(out::add);
        } catch (RuntimeException ignoredRaceArtifact) {
            // ArrayIndexOutOfBounds can occur from the race; the point is the result is unreliable
        }
        return out.size();
    }

    /** CORRECT: let the collector handle concurrency. */
    public static int collectProperly(int n) {
        return LongStream.range(0, n).parallel().boxed().toList().size();
    }
}
