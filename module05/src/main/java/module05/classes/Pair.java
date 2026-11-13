package module05.classes;

import java.util.Objects;

/** Generic class with two type parameters. Used for (accountId, balance), (currency, rate), ... */
public final class Pair<A, B> {
    private final A first;
    private final B second;

    private Pair(A first, B second) {
        this.first = Objects.requireNonNull(first);
        this.second = Objects.requireNonNull(second);
    }

    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<>(first, second);
    }

    public A first() { return first; }
    public B second() { return second; }

    public Pair<B, A> swap() {
        return new Pair<>(second, first);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) return false;
        Pair<?, ?> p = (Pair<?, ?>) o;
        return first.equals(p.first) && second.equals(p.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}
