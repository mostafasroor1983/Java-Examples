package module05.classes;

import java.util.Objects;
import java.util.UUID;

/**
 * Phantom type parameter: T is never used in a field, it exists only so that
 * TypedId<Account> and TypedId<Wallet> are different compile-time types.
 */
public final class TypedId<T> {
    private final UUID value;

    private TypedId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    public static <T> TypedId<T> of(UUID value) {
        return new TypedId<>(value);
    }

    public static <T> TypedId<T> random() {
        return new TypedId<>(UUID.randomUUID());
    }

    public UUID value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TypedId && value.equals(((TypedId<?>) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
