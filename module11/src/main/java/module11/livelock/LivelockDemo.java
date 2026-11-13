package module11.livelock;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Livelock: threads are not blocked, they keep responding to each other and make no progress.
 * Two polite settlement workers each yield to the other whenever both want the same ledger page.
 * Fix: randomised back-off or a tie-break rule (lower id wins).
 */
public final class LivelockDemo {
    private LivelockDemo() {
    }

    public static final class LedgerPage {
        volatile int owner = -1; // -1 = free
    }

    /** BAD: both detect contention and both back off, symmetrically, forever (bounded here by maxRounds). */
    public static int politeWorkersBad(int maxRounds) throws InterruptedException {
        LedgerPage page = new LedgerPage();
        AtomicInteger completed = new AtomicInteger();
        Thread w1 = worker(page, 1, 2, maxRounds, completed, false);
        Thread w2 = worker(page, 2, 1, maxRounds, completed, false);
        w1.start();
        w2.start();
        w1.join();
        w2.join();
        return completed.get();
    }

    /** GOOD: a deterministic tie-break: the lower id never yields. */
    public static int tieBreakGood(int maxRounds) throws InterruptedException {
        LedgerPage page = new LedgerPage();
        AtomicInteger completed = new AtomicInteger();
        Thread w1 = worker(page, 1, 2, maxRounds, completed, true);
        Thread w2 = worker(page, 2, 1, maxRounds, completed, true);
        w1.start();
        w2.start();
        w1.join();
        w2.join();
        return completed.get();
    }

    private static Thread worker(LedgerPage page, int me, int other, int maxRounds, AtomicInteger completed, boolean tieBreak) {
        return new Thread(() -> {
            for (int round = 0; round < maxRounds; round++) {
                page.owner = me;                 // claim
                Thread.yield();
                if (page.owner != me) {          // someone else claimed after us
                    continue;
                }
                // Symmetric politeness: if the other also wants it, step back.
                boolean otherWants = page.owner == other;
                if (!tieBreak && otherWants) {
                    page.owner = -1;
                    continue;
                }
                if (tieBreak && me > other && page.owner != me) {
                    continue; // higher id defers; lower id proceeds
                }
                completed.incrementAndGet(); // did the work
                page.owner = -1;
                return;
            }
        }, "worker-" + me);
    }
}
