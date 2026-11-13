package module09.producerconsumer;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bounded buffer with wait/notifyAll: the classic interview implementation.
 * Rules: always wait() in a loop re-checking the condition; use notifyAll() to avoid lost wake-ups.
 */
public final class WaitNotifyQueue<T> {
    private final Deque<T> items = new ArrayDeque<>();
    private final int capacity;

    public WaitNotifyQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (items.size() == capacity) {
            wait(); // releases the monitor while waiting
        }
        items.addLast(item);
        notifyAll(); // wake consumers (and other producers; they re-check and go back to sleep)
    }

    public synchronized T take() throws InterruptedException {
        while (items.isEmpty()) {
            wait();
        }
        T item = items.pollFirst();
        notifyAll();
        return item;
    }

    public synchronized int size() {
        return items.size();
    }
}
