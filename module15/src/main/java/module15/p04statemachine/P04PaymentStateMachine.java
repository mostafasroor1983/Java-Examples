package module15.p04statemachine;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * P04 - Payment lifecycle state machine with an explicit transition table.
 * CREATED -> AUTHORISED -> CAPTURED -> SETTLED; AUTHORISED -> VOIDED; CAPTURED/SETTLED -> REFUNDED; any non-final -> FAILED.
 */
public final class P04PaymentStateMachine {
    public enum State { CREATED, AUTHORISED, CAPTURED, SETTLED, VOIDED, REFUNDED, FAILED }

    public enum Event { AUTHORISE, CAPTURE, SETTLE, VOID, REFUND, FAIL }

    public static final class IllegalTransitionException extends RuntimeException {
        public IllegalTransitionException(State from, Event event) {
            super("cannot " + event + " from " + from);
        }
    }

    private static final Map<State, Map<Event, State>> TABLE = new EnumMap<>(State.class);

    static {
        put(State.CREATED, Event.AUTHORISE, State.AUTHORISED);
        put(State.CREATED, Event.FAIL, State.FAILED);
        put(State.AUTHORISED, Event.CAPTURE, State.CAPTURED);
        put(State.AUTHORISED, Event.VOID, State.VOIDED);
        put(State.AUTHORISED, Event.FAIL, State.FAILED);
        put(State.CAPTURED, Event.SETTLE, State.SETTLED);
        put(State.CAPTURED, Event.REFUND, State.REFUNDED);
        put(State.CAPTURED, Event.FAIL, State.FAILED);
        put(State.SETTLED, Event.REFUND, State.REFUNDED);
    }

    private static void put(State from, Event e, State to) {
        TABLE.computeIfAbsent(from, k -> new EnumMap<>(Event.class)).put(e, to);
    }

    public static final Set<State> FINAL_STATES = EnumSet.of(State.VOIDED, State.REFUNDED, State.FAILED);

    private State state = State.CREATED;

    public synchronized State apply(Event event) {
        State next = TABLE.getOrDefault(state, Map.of()).get(event);
        if (next == null) throw new IllegalTransitionException(state, event);
        state = next;
        return state;
    }

    public synchronized State state() {
        return state;
    }

    public synchronized boolean isFinal() {
        return FINAL_STATES.contains(state);
    }

    public static Set<Event> allowedFrom(State s) {
        return TABLE.containsKey(s) ? EnumSet.copyOf(TABLE.get(s).keySet()) : EnumSet.noneOf(Event.class);
    }
}
