package module18.ch09;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Item 58 - Prefer for-each loops to traditional for loops.
 *
 * <p>for-each hides iterators and indexes, which are the places bugs hide. The classic nested-loop bug calls
 * {@code next()} on the OUTER iterator inside the inner loop, producing wrong pairs and eventually
 * NoSuchElementException. Use explicit iterators only to remove elements (prefer {@code removeIf}), replace values
 * ({@code replaceAll}), or iterate several collections in lockstep.
 */
public final class Item58ForEach {
    private Item58ForEach() {
    }

    /** BAD: outer.next() is called once per INNER element. */
    public static List<String> pairsBuggy(List<String> cards, List<String> merchants) {
        List<String> pairs = new ArrayList<>();
        for (Iterator<String> c = cards.iterator(); c.hasNext(); ) {
            for (Iterator<String> m = merchants.iterator(); m.hasNext(); ) {
                pairs.add(c.next() + "@" + m.next());
            }
        }
        return pairs;
    }

    /** GOOD */
    public static List<String> pairs(List<String> cards, List<String> merchants) {
        List<String> pairs = new ArrayList<>();
        for (String c : cards) for (String m : merchants) pairs.add(c + "@" + m);
        return pairs;
    }
}
