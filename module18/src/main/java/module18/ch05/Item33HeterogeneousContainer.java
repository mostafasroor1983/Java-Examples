package module18.ch05;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Item 33 - Consider typesafe heterogeneous containers.
 *
 * <p>Parameterise the KEY instead of the container: a payment context holding a trace id (String), a risk score
 * (Integer) and a customer tier (enum) under {@code Class<T>} keys. {@code Class.cast} gives a checked cast, so a
 * raw-typed caller cannot sneak in the wrong type. Limitation: cannot use non-reifiable keys like {@code List<String>.class}.
 */
public final class Item33HeterogeneousContainer {
    private Item33HeterogeneousContainer() {
    }

    public static final class PaymentContext {
        private final Map<Class<?>, Object> attributes = new HashMap<>();

        public <T> void put(Class<T> type, T value) {
            attributes.put(Objects.requireNonNull(type), type.cast(value)); // dynamic check
        }

        public <T> Optional<T> get(Class<T> type) {
            return Optional.ofNullable(type.cast(attributes.get(type)));
        }
    }
}
