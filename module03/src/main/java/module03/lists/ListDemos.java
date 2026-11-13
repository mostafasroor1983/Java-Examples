package module03.lists;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * ArrayList vs LinkedList. In practice ArrayList wins almost always: cache locality,
 * O(1) random access, cheaper iteration. LinkedList only wins for iterator-based removal in the middle.
 */
public final class ListDemos {
    private ListDemos() {
    }

    /** Builds a list of n transaction ids; works for both implementations. */
    public static List<String> buildIds(List<String> target, int n) {
        for (int i = 0; i < n; i++) {
            target.add("tx-" + i);
        }
        return target;
    }

    /** BAD: removing while iterating with for-each -> ConcurrentModificationException. */
    public static void removeDeclinedBad(List<String> txs) {
        for (String tx : txs) {
            if (tx.endsWith("-declined")) {
                txs.remove(tx);
            }
        }
    }

    /** GOOD: iterator.remove() or removeIf(). */
    public static void removeDeclinedGood(List<String> txs) {
        Iterator<String> it = txs.iterator();
        while (it.hasNext()) {
            if (it.next().endsWith("-declined")) {
                it.remove();
            }
        }
    }

    public static void removeDeclinedBest(List<String> txs) {
        txs.removeIf(tx -> tx.endsWith("-declined"));
    }

    /** ArrayList grows by ~1.5x; pre-sizing avoids copies when the size is known. */
    public static ArrayList<String> presized(int expected) {
        return new ArrayList<>(expected);
    }

    public static LinkedList<String> asDequeFriendly() {
        return new LinkedList<>();
    }
}
