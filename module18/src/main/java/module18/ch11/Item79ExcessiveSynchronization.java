package module18.ch11;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Item 79 - Avoid excessive synchronization.
 *
 * <p>Never call ALIEN code (listeners, callbacks, overridable methods) while holding a lock: the callback can
 * re-enter and modify the guarded state (ConcurrentModificationException), or block on another lock and deadlock.
 * Do as little as possible inside synchronized regions. Here the ledger notifies listeners OUTSIDE its lock, and keeps
 * listeners in a {@link CopyOnWriteArrayList} so a listener can unsubscribe itself during notification. Prefer
 * leaving synchronization to the caller (document "not thread-safe") over over-synchronising internally.
 */
public final class Item79ExcessiveSynchronization {
    private Item79ExcessiveSynchronization() {
    }

    public static final class ObservableLedger {
        private final Object lock = new Object();
        @ThreadSafety.GuardedBy("lock")
        private long balanceCents;
        private final List<Consumer<Long>> listeners = new CopyOnWriteArrayList<>();

        public void subscribe(Consumer<Long> l) {
            listeners.add(l);
        }

        public void unsubscribe(Consumer<Long> l) {
            listeners.remove(l);
        }

        public void post(long cents) {
            long snapshot;
            synchronized (lock) {      // short critical section
                balanceCents += cents;
                snapshot = balanceCents;
            }
            for (Consumer<Long> l : listeners) l.accept(snapshot); // open call: no lock held
        }

        public int listenerCount() {
            return listeners.size();
        }
    }
}
