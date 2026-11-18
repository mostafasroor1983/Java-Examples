package module09.scheduling;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Periodic jobs (FX rate refresh, interest accrual, stale-payment sweeps) on a {@link ScheduledExecutorService}.
 *
 * <p>The trap, documented in {@code ScheduledExecutorService#scheduleAtFixedRate}: "If any execution of the task
 * encounters an exception, subsequent executions are suppressed." One NullPointerException at 02:00 and the
 * FX refresh silently never runs again; prices go stale with no error in the logs, because the exception is
 * stored in the never-inspected ScheduledFuture.
 *
 * <p>{@link #scheduleGuarded} wraps the task so every exception is caught, counted and reported, and the schedule
 * keeps going. Use fixed DELAY for jobs that call external systems: fixed RATE tries to "catch up" after a slow run
 * and fires back-to-back executions (it never runs them concurrently, but it does burst).
 */
public final class ResilientScheduler implements AutoCloseable {

    public record JobStats(long runs, long failures) {
    }

    private final ScheduledExecutorService scheduler;
    private final Consumer<Throwable> errorReporter;
    private final Map<String, AtomicLong[]> stats = new ConcurrentHashMap<>();

    public ResilientScheduler(int threads, Consumer<Throwable> errorReporter) {
        this.scheduler = Executors.newScheduledThreadPool(threads, r -> {
            Thread t = new Thread(r, "scheduler");
            t.setDaemon(true);
            return t;
        });
        this.errorReporter = Objects.requireNonNull(errorReporter, "errorReporter");
    }

    /** BAD on purpose: the first exception cancels all future runs. Returned future lets tests observe it. */
    public ScheduledFuture<?> scheduleNaive(Runnable task, Duration period) {
        return scheduler.scheduleWithFixedDelay(task, 0, period.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** GOOD: catches RuntimeException per run, reports it, and keeps the schedule alive. */
    public ScheduledFuture<?> scheduleGuarded(String jobName, Runnable task, Duration period) {
        AtomicLong[] s = stats.computeIfAbsent(jobName, k -> new AtomicLong[]{new AtomicLong(), new AtomicLong()});
        Runnable guarded = () -> {
            s[0].incrementAndGet();
            try {
                task.run();
            } catch (RuntimeException e) {
                s[1].incrementAndGet();
                errorReporter.accept(e); // alert on this metric; a failing job must be visible
            }
            // Errors (OOM, StackOverflow) are deliberately NOT caught: the JVM is in an unknown state.
        };
        return scheduler.scheduleWithFixedDelay(guarded, 0, period.toMillis(), TimeUnit.MILLISECONDS);
    }

    public JobStats stats(String jobName) {
        AtomicLong[] s = stats.get(jobName);
        return s == null ? new JobStats(0, 0) : new JobStats(s[0].get(), s[1].get());
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
