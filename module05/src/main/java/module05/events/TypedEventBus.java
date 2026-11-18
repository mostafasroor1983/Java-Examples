package module05.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-process event bus built as a type-safe heterogeneous container (Effective Java item 33).
 *
 * <p>{@code subscribe(Class<E>, Consumer<? super E>)} ties the handler's parameter type to the key's type; the
 * unchecked cast in {@link #publish} is therefore provably safe. PECS in action: a {@code Consumer<PaymentEvent>}
 * (super) may subscribe to {@code PaymentSettled} (extends).
 *
 * <p>Dispatch walks the event's class AND its interfaces/superclasses, so an audit handler subscribed to the
 * root event type sees every event.
 *
 * <p>Failure isolation: one failing handler must not stop the audit handler from running, so exceptions are
 * collected and returned instead of propagated. Note this bus is synchronous and in-memory: there is no durability,
 * and no delivery to other services. Cross-service events go through an outbox and a broker.
 */
public final class TypedEventBus<B> {
    private final Map<Class<?>, List<Consumer<Object>>> handlers = new ConcurrentHashMap<>();
    private final Class<B> baseType;

    public TypedEventBus(Class<B> baseType) {
        this.baseType = Objects.requireNonNull(baseType, "baseType");
    }

    @SuppressWarnings("unchecked") // safe: handler accepts E (or a supertype of E) and is only invoked with E instances
    public <E extends B> void subscribe(Class<E> type, Consumer<? super E> handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add((Consumer<Object>) handler);
    }

    /** Publishes to every handler registered for the event's type hierarchy; returns handler failures (never throws). */
    public List<RuntimeException> publish(B event) {
        Objects.requireNonNull(event, "event");
        List<RuntimeException> failures = new ArrayList<>();
        for (Class<?> type : hierarchyOf(event.getClass())) {
            for (Consumer<Object> h : handlers.getOrDefault(type, List.of())) {
                try {
                    h.accept(event);
                } catch (RuntimeException e) {
                    failures.add(e);
                }
            }
        }
        return failures;
    }

    private List<Class<?>> hierarchyOf(Class<?> type) {
        List<Class<?>> out = new ArrayList<>();
        collect(type, out);
        return out;
    }

    private void collect(Class<?> type, List<Class<?>> out) {
        if (type == null || out.contains(type) || !baseType.isAssignableFrom(type)) return;
        out.add(type);
        collect(type.getSuperclass(), out);
        for (Class<?> i : type.getInterfaces()) collect(i, out);
    }
}
