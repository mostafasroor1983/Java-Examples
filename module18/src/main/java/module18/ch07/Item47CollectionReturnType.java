package module18.ch07;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Item 47 - Prefer Collection to Stream as a return type.
 *
 * <p>Callers may want to iterate (for-each) or stream. {@link java.util.Collection} supports both
 * ({@code collection.stream()}), while a returned Stream cannot be used in a for-each loop without an adapter.
 * Return a List/Collection when the result fits in memory; return a Stream only for huge or lazily produced
 * sequences (and document that it must be closed if it holds resources). Adapters are shown for completeness.
 */
public final class Item47CollectionReturnType {
    private Item47CollectionReturnType() {
    }

    public record StatementLine(String date, String description, String amount) { }

    /** GOOD: a List works for loops, streams, size(), contains(). */
    public static List<StatementLine> statementLines() {
        return List.of(new StatementLine("2024-03-01", "Salary", "+3000.00"),
                new StatementLine("2024-03-02", "Rent", "-1200.00"));
    }

    public static <E> Iterable<E> iterableOf(Stream<E> stream) {
        return stream::iterator;
    }

    public static <E> Stream<E> streamOf(Iterable<E> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
