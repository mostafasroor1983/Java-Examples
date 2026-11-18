package module14.benchmark;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * A deliberately small benchmark harness that shows WHAT a real harness has to do, and why you should use JMH
 * ({@code org.openjdk.jmh}) for any number you intend to act on.
 *
 * <p>What it handles:
 * <ol>
 *   <li><b>Warm-up</b>: early iterations run in the interpreter/C1; measuring them reports JIT compilation, not code.</li>
 *   <li><b>Dead-code elimination</b>: results are folded into a "blackhole" sink, so the JIT cannot delete the work.</li>
 *   <li><b>Distribution, not average</b>: reports median and p99, because latency SLAs are percentiles.</li>
 * </ol>
 * What it does NOT handle (JMH does): forking fresh JVMs, constant folding of inputs, loop-unrolling artefacts, GC noise
 * isolation, CPU frequency scaling, and statistical error bounds. Never put timing assertions in unit tests: CI hardware varies.
 */
public final class MiniBenchmark {
    public record Result(String name, int iterations, long medianNanos, long p99Nanos, long sink) {
    }

    private long sink; // blackhole: written after each run so results stay "used"

    public Result run(String name, int warmupIterations, int measuredIterations, LongSupplier workload) {
        Objects.requireNonNull(workload);
        if (warmupIterations < 0 || measuredIterations < 1) throw new IllegalArgumentException("bad iteration counts");
        for (int i = 0; i < warmupIterations; i++) sink ^= workload.getAsLong();
        long[] samples = new long[measuredIterations];
        for (int i = 0; i < measuredIterations; i++) {
            long t0 = System.nanoTime();
            long r = workload.getAsLong();
            samples[i] = System.nanoTime() - t0;
            sink ^= r;
        }
        Arrays.sort(samples);
        return new Result(name, measuredIterations, percentile(samples, 50), percentile(samples, 99), sink);
    }

    /** Nearest-rank percentile on a sorted array. */
    static long percentile(long[] sorted, int p) {
        if (sorted.length == 0) throw new IllegalArgumentException("no samples");
        int rank = (int) Math.ceil(p / 100.0 * sorted.length);
        return sorted[Math.max(0, rank - 1)];
    }
}
