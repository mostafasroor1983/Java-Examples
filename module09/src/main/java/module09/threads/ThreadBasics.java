package module09.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Creating, naming, joining, daemon threads, interruption, uncaught exception handlers. */
public final class ThreadBasics {
    private ThreadBasics() {
    }

    /** Run tasks each on its own thread and join them all. Returns their names in start order. */
    public static List<String> runAndJoin(List<Runnable> tasks) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            Thread t = new Thread(tasks.get(i), "settlement-worker-" + i);
            names.add(t.getName());
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();
        return names;
    }

    /** Cooperative cancellation: a worker that stops when interrupted. Returns iterations completed. */
    public static int interruptibleWorker(long spinUntilInterrupted) throws InterruptedException {
        int[] iterations = {0};
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                iterations[0]++;
                if (iterations[0] >= spinUntilInterrupted) {
                    Thread.currentThread().interrupt(); // self-cancel for determinism in tests
                }
            }
        });
        t.start();
        t.join();
        return iterations[0];
    }

    /** Blocking calls throw InterruptedException; the flag is cleared when thrown. */
    public static boolean sleepWasInterrupted(Thread sleeper) throws InterruptedException {
        AtomicReference<Boolean> interrupted = new AtomicReference<>(false);
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
        });
        t.start();
        t.interrupt();
        t.join();
        return interrupted.get();
    }

    /** Uncaught exceptions on a thread kill only that thread; the handler sees them. */
    public static Throwable captureUncaught(Runnable failing) throws InterruptedException {
        AtomicReference<Throwable> captured = new AtomicReference<>();
        Thread t = new Thread(failing);
        t.setUncaughtExceptionHandler((thread, ex) -> captured.set(ex));
        t.start();
        t.join();
        return captured.get();
    }

    public static boolean isDaemon(boolean daemon) {
        Thread t = new Thread(() -> { });
        t.setDaemon(daemon);
        return t.isDaemon();
    }
}
