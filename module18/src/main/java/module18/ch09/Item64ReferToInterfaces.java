package module18.ch09;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Item 64 - Refer to objects by their interfaces.
 *
 * <p>Declare variables, parameters and return types with interfaces ({@code List}, {@code Map}) so the implementation
 * can change without touching callers. Java 21 adds {@link SequencedCollection} (JEP 431): one interface for
 * "ordered with first/last", implemented by List, Deque, LinkedHashSet and more, so methods can accept any of them.
 * Use a class type only when you need class-specific behaviour (e.g. {@code PriorityQueue} ordering).
 */
public final class Item64ReferToInterfaces {
    private Item64ReferToInterfaces() {
    }

    /** Java 21: works for List, Deque, LinkedHashSet ... */
    public static <E> String firstAndLast(SequencedCollection<E> history) {
        if (history.isEmpty()) return "empty";
        return history.getFirst() + ".." + history.getLast();
    }

    public static List<String> newHistory() {
        List<String> h = new ArrayList<>(); // declared as the interface
        h.add("T1");
        h.add("T2");
        return h;
    }

    public static SequencedCollection<String> uniqueHistory() {
        return new LinkedHashSet<>(List.of("T1", "T2", "T1"));
    }
}
