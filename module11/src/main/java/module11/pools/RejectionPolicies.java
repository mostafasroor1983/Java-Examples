package module11.pools;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The four built-ins plus a custom "count and drop" used for metrics.
 * AbortPolicy (default): throw RejectedExecutionException -> caller sheds load.
 * CallerRunsPolicy: the submitting thread runs the task -> natural back-pressure, but blocks e.g. a request thread.
 * DiscardPolicy / DiscardOldestPolicy: silent loss -> almost never acceptable for payments.
 */
public final class RejectionPolicies {
    private RejectionPolicies() {
    }

    public static RejectedExecutionHandler abort() {
        return new ThreadPoolExecutor.AbortPolicy();
    }

    public static RejectedExecutionHandler callerRuns() {
        return new ThreadPoolExecutor.CallerRunsPolicy();
    }

    public static RejectedExecutionHandler discard() {
        return new ThreadPoolExecutor.DiscardPolicy();
    }

    public static final class CountingDrop implements RejectedExecutionHandler {
        private final AtomicInteger dropped = new AtomicInteger();

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            dropped.incrementAndGet();
        }

        public int dropped() {
            return dropped.get();
        }
    }
}
