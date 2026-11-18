package module18.ch05;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/**
 * Item 31 - Use bounded wildcards to increase API flexibility.
 *
 * <p>PECS: Producer-Extends, Consumer-Super. {@code pushAll} READS from its argument (producer) so it takes
 * {@code Iterable<? extends E>}; {@code drainTo} WRITES into its argument (consumer) so it takes
 * {@code Collection<? super E>}. With plain {@code E} a {@code Buffer<Payment>} could not accept a
 * {@code List<CardPayment>} nor drain into a {@code List<Object>}. Never use wildcards in return types.
 */
public final class Item31BoundedWildcards {
    private Item31BoundedWildcards() {
    }

    public interface Payment {
        String id();
    }

    public record CardPayment(String id) implements Payment { }

    public record BankTransfer(String id) implements Payment { }

    public static final class Buffer<E> {
        private final Deque<E> items = new ArrayDeque<>();

        public void pushAll(Iterable<? extends E> src) {
            for (E e : src) items.push(e);
        }

        public void drainTo(Collection<? super E> dst) {
            while (!items.isEmpty()) dst.add(items.pop());
        }

        public int size() {
            return items.size();
        }
    }
}
