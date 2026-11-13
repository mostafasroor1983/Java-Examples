package module09.producerconsumer;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * BAD: `if` instead of `while` around wait() and notify() instead of notifyAll().
 * Spurious wake-ups or a wake-up stolen by another producer lead to take() on an empty queue
 * (NoSuchElementException / null) or put() beyond capacity.
 */
public final class BrokenWaitNotifyQueue<T> {
    private final Deque<T> items = new ArrayDeque<>();
    private final int capacity;

    public BrokenWaitNotifyQueue(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        if (items.size() == capacity) {
            wait();
        }
        items.addLast(item); // may exceed capacity if woken while still full
        notify();
    }

    public synchronized T take() throws InterruptedException {
        if (items.isEmpty()) {
            wait();
        }
        T item = items.pollFirst(); // may be null if woken while still empty
        notify();
        return item;
    }

    public synchronized int size() {
        return items.size();
    }
}
