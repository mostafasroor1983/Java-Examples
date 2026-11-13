package module05.bounds;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/** Upper bounds, multiple bounds, recursive bounds. */
public final class Bounds {
    private Bounds() {
    }

    /** Multiple bounds: T must be both Summable and Comparable. Class bound first, interfaces after `&`. */
    public static <T extends Summable<T> & Comparable<T>> T sumIfAscending(List<T> items) {
        if (items.isEmpty()) throw new IllegalArgumentException("empty");
        Iterator<T> it = items.iterator();
        T acc = it.next();
        T prev = acc;
        while (it.hasNext()) {
            T cur = it.next();
            if (cur.compareTo(prev) < 0) throw new IllegalArgumentException("not ascending at " + cur);
            acc = acc.plus(cur);
            prev = cur;
        }
        return acc;
    }

    /** Lower-bounded Comparable so that subclasses of a comparable base still work. */
    public static <T extends Comparable<? super T>> boolean isSorted(Collection<? extends T> items) {
        T prev = null;
        for (T t : items) {
            if (prev != null && t.compareTo(prev) < 0) return false;
            prev = t;
        }
        return true;
    }

    /** Recursive bound (the "Enum<E extends Enum<E>>" pattern) for fluent builders. */
    public abstract static class PaymentBuilder<B extends PaymentBuilder<B>> {
        protected String id;
        protected Money amount;

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        public B id(String id) {
            this.id = id;
            return self();
        }

        public B amount(Money amount) {
            this.amount = amount;
            return self();
        }
    }

    public static final class CardPaymentBuilder extends PaymentBuilder<CardPaymentBuilder> {
        private String pan;

        public CardPaymentBuilder pan(String pan) {
            this.pan = pan;
            return this;
        }

        public String build() {
            return "CARD " + id + " " + amount.amount() + " " + amount.currency() + " " + pan;
        }
    }
}
