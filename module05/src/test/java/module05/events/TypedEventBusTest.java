package module05.events;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class TypedEventBusTest {
    sealed interface PaymentEvent permits PaymentSettled, PaymentFailed { String id(); }
    record PaymentSettled(String id) implements PaymentEvent { }
    record PaymentFailed(String id, String reason) implements PaymentEvent { }

    @Test
    void dispatchesByExactTypeAndSupertype() {
        var bus = new TypedEventBus<>(PaymentEvent.class);
        List<String> settled = new ArrayList<>(), audit = new ArrayList<>();
        bus.subscribe(PaymentSettled.class, e -> settled.add(e.id()));
        Consumer<PaymentEvent> auditor = e -> audit.add(e.getClass().getSimpleName() + ":" + e.id());
        bus.subscribe(PaymentEvent.class, auditor);
        bus.subscribe(PaymentSettled.class, auditor); // PECS: Consumer<PaymentEvent> accepted for PaymentSettled

        assertTrue(bus.publish(new PaymentSettled("P1")).isEmpty());
        bus.publish(new PaymentFailed("P2", "declined"));
        assertEquals(List.of("P1"), settled);
        assertEquals(List.of("PaymentSettled:P1", "PaymentSettled:P1", "PaymentFailed:P2"), audit);
    }

    @Test
    void failingHandlerDoesNotStopOthers() {
        var bus = new TypedEventBus<>(PaymentEvent.class);
        List<String> audit = new ArrayList<>();
        bus.subscribe(PaymentFailed.class, e -> { throw new IllegalStateException("notifier down"); });
        bus.subscribe(PaymentEvent.class, e -> audit.add(e.id()));
        var failures = bus.publish(new PaymentFailed("P9", "fraud"));
        assertEquals(1, failures.size());
        assertEquals(List.of("P9"), audit, "audit trail still written");
    }
}
