package module14.allocation;

import java.lang.management.ManagementFactory;

/**
 * Measures bytes allocated by the CURRENT thread while running a task, using the HotSpot-specific
 * {@code com.sun.management.ThreadMXBean#getCurrentThreadAllocatedBytes} (JDK 14+; the per-thread-id form exists since 6u25).
 *
 * <p>Allocation rate is the main driver of young-GC frequency. In a payment service, cutting allocation per request
 * (boxing, BigDecimal temporaries, string building for logs that are filtered out anyway) often improves p99 latency
 * more than any CPU micro-optimisation. In production, use JFR ({@code jdk.ObjectAllocationSample}) or async-profiler
 * in allocation mode instead of instrumenting code.
 */
public final class AllocationProbe {
    private AllocationProbe() {
    }

    public static boolean supported() {
        return ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean t
                && t.isThreadAllocatedMemorySupported() && t.isThreadAllocatedMemoryEnabled();
    }

    /** @return bytes allocated by this thread during {@code task}, or -1 if unsupported on this JVM */
    public static long allocatedBytes(Runnable task) {
        if (!(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean mx) || !supported()) {
            task.run();
            return -1;
        }
        long before = mx.getCurrentThreadAllocatedBytes();
        task.run();
        return mx.getCurrentThreadAllocatedBytes() - before;
    }
}
