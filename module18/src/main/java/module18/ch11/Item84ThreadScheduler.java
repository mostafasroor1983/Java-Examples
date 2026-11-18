package module18.ch11;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Item 84 - Don't depend on the thread scheduler.
 *
 * <p>Correctness must not rely on thread priorities, {@code Thread.yield}, or timing. Busy-waiting ("spin until the
 * counter reaches zero") burns CPU and starves the thread that would make progress, and its behaviour differs between
 * machines. Keep runnable threads few (each should do useful work), and block properly using a synchronizer. Also
 * do not "fix" a concurrency bug with yield or sleep: it only hides it on your laptop.
 */
public final class Item84ThreadScheduler {
    private Item84ThreadScheduler() {
    }

    /** BAD: busy-waiting latch (do not use; shown for contrast). */
    public static final class SpinningLatch {
        private int count;

        public SpinningLatch(int count) {
            this.count = count;
        }

        public void await() {
            while (true) {
                synchronized (this) {
                    if (count == 0) return;
                }
                // spins, wasting a core; Thread.yield() here would not make it correct, only "less bad"
            }
        }

        public synchronized void countDown() {
            if (count != 0) count--;
        }
    }

    /** GOOD: blocks without consuming CPU, with a timeout. */
    public static boolean awaitSettlementFiles(CountDownLatch filesReceived, long timeoutMillis) throws InterruptedException {
        return filesReceived.await(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}
