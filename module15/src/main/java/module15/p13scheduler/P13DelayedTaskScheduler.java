package module15.p13scheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.LongSupplier;

/**
 * P13 - Deterministic delayed-task scheduler: tasks run in due-time order when `runDue()` is called with a
 * controllable clock (no real threads, no sleeps). Supports cancel and fixed-rate rescheduling.
 * The design mirrors ScheduledThreadPoolExecutor's DelayedWorkQueue.
 */
public final class P13DelayedTaskScheduler {
    private static final class Task implements Comparable<Task> {
        final long id;
        long dueNanos;
        final Runnable body;
        final long periodNanos; // 0 = one-shot
        volatile boolean cancelled;

        Task(long id, long dueNanos, Runnable body, long periodNanos) {
            this.id = id;
            this.dueNanos = dueNanos;
            this.body = body;
            this.periodNanos = periodNanos;
        }

        @Override
        public int compareTo(Task o) {
            int c = Long.compare(dueNanos, o.dueNanos);
            return c != 0 ? c : Long.compare(id, o.id); // FIFO for equal due times
        }
    }

    public interface Handle {
        boolean cancel();

        boolean isCancelled();
    }

    private final LongSupplier nanoClock;
    private final PriorityQueue<Task> queue = new PriorityQueue<>();
    private long nextId;

    public P13DelayedTaskScheduler(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
    }

    public synchronized Handle schedule(Runnable body, Duration delay) {
        return enqueue(body, delay, Duration.ZERO);
    }

    public synchronized Handle scheduleAtFixedRate(Runnable body, Duration initialDelay, Duration period) {
        if (period.isZero() || period.isNegative()) throw new IllegalArgumentException("period must be positive");
        return enqueue(body, initialDelay, period);
    }

    private Handle enqueue(Runnable body, Duration delay, Duration period) {
        if (delay.isNegative()) throw new IllegalArgumentException("delay must be >= 0");
        Task t = new Task(nextId++, nanoClock.getAsLong() + delay.toNanos(), body, period.toNanos());
        queue.add(t);
        return new Handle() {
            public boolean cancel() {
                if (t.cancelled) return false;
                t.cancelled = true;
                synchronized (P13DelayedTaskScheduler.this) {
                    queue.remove(t);
                }
                return true;
            }

            public boolean isCancelled() {
                return t.cancelled;
            }
        };
    }

    /** Runs every task whose due time <= now, in order; returns how many ran. Periodic tasks are re-queued. */
    public int runDue() {
        List<Task> due = new ArrayList<>();
        long now = nanoClock.getAsLong();
        synchronized (this) {
            while (!queue.isEmpty() && queue.peek().dueNanos <= now) due.add(queue.poll());
        }
        int ran = 0;
        for (Task t : due) {
            if (t.cancelled) continue;
            t.body.run();
            ran++;
            if (t.periodNanos > 0) {
                t.dueNanos += t.periodNanos; // fixed rate: schedule from the previous due time, not from now
                synchronized (this) {
                    if (!t.cancelled) queue.add(t);
                }
            }
        }
        return ran;
    }

    public synchronized int pending() {
        return queue.size();
    }

    /** Nanos until the next task is due, or -1 if none (what a worker thread would sleep for). */
    public synchronized long nanosUntilNext() {
        return queue.isEmpty() ? -1 : Math.max(0, queue.peek().dueNanos - nanoClock.getAsLong());
    }
}
