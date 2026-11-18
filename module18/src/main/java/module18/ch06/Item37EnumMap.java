package module18.ch06;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Item 37 - Use EnumMap instead of ordinal indexing.
 *
 * <p>Indexing an array by {@code ordinal()} (e.g. {@code totals[channel.ordinal()]}) loses type safety and breaks when
 * constants are reordered. {@link EnumMap} is array-backed (as fast), type-safe and prints nicely. Collectors can build
 * one directly via {@code groupingBy(..., () -> new EnumMap<>(K.class), ...)}. Nested EnumMaps model state transition
 * tables cleanly.
 */
public final class Item37EnumMap {
    private Item37EnumMap() {
    }

    public enum Channel { CARD, SEPA, SWIFT, WALLET }

    public enum State { CREATED, AUTHORISED, SETTLED, FAILED }

    public enum Event { AUTHORISE, SETTLE, FAIL }

    public record Payment(Channel channel, BigDecimal amount) { }

    public static Map<Channel, BigDecimal> totalsByChannel(List<Payment> payments) {
        return payments.stream().collect(Collectors.groupingBy(Payment::channel, () -> new EnumMap<>(Channel.class),
                Collectors.reducing(BigDecimal.ZERO, Payment::amount, BigDecimal::add)));
    }

    /** Transition table: state -> (event -> next state). */
    private static final Map<State, Map<Event, State>> TRANSITIONS = new EnumMap<>(State.class);

    static {
        for (State s : State.values()) TRANSITIONS.put(s, new EnumMap<>(Event.class));
        TRANSITIONS.get(State.CREATED).put(Event.AUTHORISE, State.AUTHORISED);
        TRANSITIONS.get(State.CREATED).put(Event.FAIL, State.FAILED);
        TRANSITIONS.get(State.AUTHORISED).put(Event.SETTLE, State.SETTLED);
        TRANSITIONS.get(State.AUTHORISED).put(Event.FAIL, State.FAILED);
    }

    public static State apply(State from, Event event) {
        State next = TRANSITIONS.get(from).get(event);
        if (next == null) throw new IllegalStateException(event + " not allowed in " + from);
        return next;
    }
}
