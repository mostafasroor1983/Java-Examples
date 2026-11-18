package module11.diagnostics;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Programmatic deadlock detection via {@link ThreadMXBean}: the same information {@code jstack} prints under
 * "Found one Java-level deadlock". Detects cycles over BOTH intrinsic monitors ({@code synchronized}) and
 * {@code java.util.concurrent} ownable synchronizers (ReentrantLock, ReentrantReadWriteLock).
 *
 * <p>Typical production use: a watchdog thread runs {@link #detect()} every N seconds and, on a hit, logs the report,
 * increments a metric and fails the health check, so the orchestrator restarts the pod. A deadlocked JVM still answers
 * TCP health probes, so without this check it can sit "healthy" while every payment times out.
 *
 * <p>Limitations: no detection of deadlocks across JVMs or in the database (the DB reports those), livelocks, or
 * threads waiting forever on a Condition/latch that nobody will signal (that is not a lock cycle).
 */
public final class DeadlockDetector {
    private final ThreadMXBean mx = ManagementFactory.getThreadMXBean();

    public record DeadlockedThread(String threadName, String waitingFor, String heldBy) {
    }

    public List<DeadlockedThread> detect() {
        long[] ids = mx.isSynchronizerUsageSupported() ? mx.findDeadlockedThreads() : mx.findMonitorDeadlockedThreads();
        if (ids == null) return List.of();
        List<DeadlockedThread> out = new ArrayList<>();
        for (ThreadInfo info : mx.getThreadInfo(ids, true, true)) {
            if (info == null) continue; // thread terminated between the two calls
            LockInfo waiting = info.getLockInfo();
            out.add(new DeadlockedThread(info.getThreadName(),
                    waiting == null ? "?" : waiting.getClassName(),
                    info.getLockOwnerName()));
        }
        return List.copyOf(out);
    }
}
