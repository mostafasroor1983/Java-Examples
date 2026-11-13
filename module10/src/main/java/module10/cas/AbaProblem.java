package module10.cas;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * ABA: thread 1 reads A, is preempted; thread 2 changes A->B->A; thread 1's CAS(A, C) succeeds although
 * the world changed underneath it. With references this can corrupt a lock-free stack (a node was freed and reused).
 * Fix: AtomicStampedReference (value + version stamp) - the stamp differs even if the value is back to A.
 */
public final class AbaProblem {
    private AbaProblem() {
    }

    public record Balance(long cents) {
    }

    /** Demonstrates the vulnerability: CAS cannot tell A from "A again". */
    public static boolean casSucceedsDespiteAba() {
        Balance a = new Balance(100);
        Balance b = new Balance(50);
        AtomicReference<Balance> ref = new AtomicReference<>(a);

        Balance observed = ref.get();             // thread 1 reads A
        ref.compareAndSet(a, b);                  // thread 2: A -> B
        ref.compareAndSet(b, a);                  // thread 2: B -> A (same instance!)
        return ref.compareAndSet(observed, new Balance(0)); // thread 1's CAS succeeds - ABA
    }

    /** Stamped reference detects the intervening changes. */
    public static boolean stampedCasDetectsAba() {
        Balance a = new Balance(100);
        Balance b = new Balance(50);
        AtomicStampedReference<Balance> ref = new AtomicStampedReference<>(a, 0);

        int[] stamp = new int[1];
        Balance observed = ref.get(stamp);        // thread 1 reads (A, 0)
        ref.compareAndSet(a, b, 0, 1);            // thread 2: (A,0) -> (B,1)
        ref.compareAndSet(b, a, 1, 2);            // thread 2: (B,1) -> (A,2)
        return ref.compareAndSet(observed, new Balance(0), stamp[0], stamp[0] + 1); // fails: stamp is 2, not 0
    }

    /** Simple lock-free stack using CAS on the head; a classic ABA victim without GC, safe in Java thanks to GC. */
    public static final class LockFreeStack<T> {
        private record Node<T>(T value, Node<T> next) {
        }

        private final AtomicReference<Node<T>> head = new AtomicReference<>();

        public void push(T value) {
            Node<T> newHead;
            Node<T> oldHead;
            do {
                oldHead = head.get();
                newHead = new Node<>(value, oldHead);
            } while (!head.compareAndSet(oldHead, newHead));
        }

        public T pop() {
            Node<T> oldHead;
            Node<T> newHead;
            do {
                oldHead = head.get();
                if (oldHead == null) return null;
                newHead = oldHead.next();
            } while (!head.compareAndSet(oldHead, newHead));
            return oldHead.value();
        }

        public boolean isEmpty() {
            return head.get() == null;
        }
    }
}
