package module18.ch10;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Item 76 - Strive for failure atomicity.
 *
 * <p>A failed method call should leave the object in the state it was in before the call. Techniques: immutable
 * objects (nothing to roll back); check parameters and state BEFORE mutating; order the computation so anything that
 * can fail happens first; work on a temporary copy and swap it in at the end. {@link BrokenBatch} posts entries one by
 * one and leaves half a batch on failure; {@link AtomicBatch} validates everything first, then commits.
 */
public final class Item76FailureAtomicity {
    private Item76FailureAtomicity() {
    }

    public static final class BrokenBatch {
        final List<BigDecimal> postings = new ArrayList<>();

        public void postAll(List<BigDecimal> amounts) {
            for (BigDecimal a : amounts) {
                if (a.signum() == 0) throw new IllegalArgumentException("zero posting");
                postings.add(a); // earlier entries already applied when a later one fails
            }
        }

        public int size() {
            return postings.size();
        }
    }

    public static final class AtomicBatch {
        private List<BigDecimal> postings = List.of();

        public void postAll(List<BigDecimal> amounts) {
            for (BigDecimal a : amounts) if (a.signum() == 0) throw new IllegalArgumentException("zero posting");
            List<BigDecimal> next = new ArrayList<>(postings); // temporary copy
            next.addAll(amounts);
            postings = List.copyOf(next);                      // single swap at the end
        }

        public int size() {
            return postings.size();
        }
    }
}
