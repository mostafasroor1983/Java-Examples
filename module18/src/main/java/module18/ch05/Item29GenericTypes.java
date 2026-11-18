package module18.ch05;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Item 29 - Favor generic types.
 *
 * <p>A container written for {@code Object} forces every caller to cast and allows the wrong type in. Making it
 * generic moves the checks to compile time at zero runtime cost. Here: a bounded FIFO of pending items (payments,
 * events, retries) with back-pressure via a boolean {@code offer}.
 */
public final class Item29GenericTypes {
    private Item29GenericTypes() {
    }

    public static final class BoundedQueue<E> {
        private final Deque<E> items = new ArrayDeque<>();
        private final int capacity;

        public BoundedQueue(int capacity) {
            if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
            this.capacity = capacity;
        }

        public boolean offer(E e) {
            java.util.Objects.requireNonNull(e);
            if (items.size() == capacity) return false;
            return items.offerLast(e);
        }

        public Optional<E> poll() {
            return Optional.ofNullable(items.pollFirst());
        }

        public int size() {
            return items.size();
        }
    }
}
