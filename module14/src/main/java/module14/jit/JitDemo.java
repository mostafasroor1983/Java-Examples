package module14.jit;

import java.math.BigDecimal;

/**
 * JIT: bytecode is interpreted first; hot methods are compiled by C1 (fast, light) then C2 (slow, optimised).
 * Consequences: warm-up matters for latency-sensitive services, micro-benchmarks need JMH, dead code is eliminated,
 * monomorphic call sites get inlined, megamorphic ones do not.
 */
public final class JitDemo {
    private JitDemo() {
    }

    public interface Fee {
        long apply(long cents);
    }

    public static final class Percent implements Fee {
        public long apply(long cents) {
            return cents / 100;
        }
    }

    public static final class Flat implements Fee {
        public long apply(long cents) {
            return 20;
        }
    }

    public static final class Tiered implements Fee {
        public long apply(long cents) {
            return cents > 10_000 ? 50 : 10;
        }
    }

    /** Monomorphic: one receiver type at the call site -> inlined after warm-up. */
    public static long monomorphic(Fee fee, long iterations) {
        long acc = 0;
        for (long i = 0; i < iterations; i++) acc += fee.apply(i);
        return acc;
    }

    /** Megamorphic: three+ types at one site -> vtable/itable dispatch, no inlining. Same result, slower steady state. */
    public static long megamorphic(Fee[] fees, long iterations) {
        long acc = 0;
        for (long i = 0; i < iterations; i++) acc += fees[(int) (i % fees.length)].apply(i);
        return acc;
    }

    /** Measures how the per-call time falls as the method gets compiled. Returns nanos per batch for each batch. */
    public static long[] warmupCurve(int batches, int batchSize) {
        long[] perBatch = new long[batches];
        Fee fee = new Percent();
        for (int b = 0; b < batches; b++) {
            long start = System.nanoTime();
            monomorphic(fee, batchSize);
            perBatch[b] = System.nanoTime() - start;
        }
        return perBatch;
    }

    /** Dead-code elimination trap: without using the result, the JIT may remove the whole loop. Always consume results. */
    public static BigDecimal consumed(int n) {
        BigDecimal acc = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) acc = acc.add(BigDecimal.ONE);
        return acc; // returned => cannot be eliminated
    }

    public static String compilerInfo() {
        return System.getProperty("java.vm.name") + " / " + System.getProperty("java.vm.version");
    }
}
