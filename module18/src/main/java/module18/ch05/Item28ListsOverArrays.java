package module18.ch05;

import java.util.Collection;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Item 28 - Prefer lists to arrays.
 *
 * <p>Arrays are covariant and reified: {@code Object[] o = new String[1]; o[0] = 1;} compiles and fails at runtime
 * with ArrayStoreException. Generics are invariant and erased: {@code List<Object> o = new ArrayList<String>()} does
 * not compile. Mixing the two produces unchecked warnings and generic array creation errors. Prefer {@code List<E>}.
 * {@link RandomGenerator} is the Java 17 random API (JEP 356).
 */
public final class Item28ListsOverArrays {
    private Item28ListsOverArrays() {
    }

    /** Demonstrates the runtime failure arrays allow. */
    public static void covariantArrayStore() {
        Object[] amounts = new String[1];
        amounts[0] = 42; // ArrayStoreException at runtime
    }

    /** Picks a random payment provider for load spreading; a List avoids generic-array problems. */
    public static final class Chooser<T> {
        private final List<T> choices;
        private final RandomGenerator random;

        public Chooser(Collection<? extends T> choices, RandomGenerator random) {
            if (choices.isEmpty()) throw new IllegalArgumentException("no choices");
            this.choices = List.copyOf(choices);
            this.random = random;
        }

        public T choose() {
            return choices.get(random.nextInt(choices.size()));
        }
    }
}
