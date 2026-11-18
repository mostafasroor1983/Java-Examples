package module18.ch09;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

/**
 * Item 67 - Optimize judiciously.
 *
 * <p>Write good programs, not fast ones: clean architecture lets you optimise the real bottleneck later. Avoid design
 * decisions that limit performance forever (mutable public types that force defensive copies everywhere, chatty
 * APIs). Measure before and after with a profiler or JMH; most "optimisations" do nothing or hurt. The helper below
 * measures; it is NOT a benchmark harness (no warm-up, no forks: see module14).
 */
public final class Item67OptimizeJudiciously {
    private Item67OptimizeJudiciously() {
    }

    public record Measured<T>(T result, long nanos) { }

    public static <T> Measured<T> measure(Supplier<T> work) {
        long t0 = System.nanoTime();
        T r = work.get();
        return new Measured<>(r, System.nanoTime() - t0);
    }

    /** Clear first: one obvious loop. Optimise only if a profile says this matters. */
    public static BigDecimal total(List<BigDecimal> amounts) {
        BigDecimal t = BigDecimal.ZERO;
        for (BigDecimal a : amounts) t = t.add(a);
        return t;
    }
}
