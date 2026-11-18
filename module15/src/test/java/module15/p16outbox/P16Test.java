package module15.p16outbox;

import module15.p16outbox.P16TransactionalOutbox.Database;
import module15.p16outbox.P16TransactionalOutbox.IdempotentConsumer;
import module15.p16outbox.P16TransactionalOutbox.Relay;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class P16Test {

    @Test
    void stateAndEventAreAtomic() {
        Database db = new Database();
        db.open("A", new BigDecimal("100"));
        assertThrows(IllegalStateException.class, () -> db.debitWithEvent("A", BigDecimal.TEN, true));
        assertEquals(new BigDecimal("100"), db.balance("A"), "rolled back");
        assertEquals(0, db.outboxSize(), "no orphan event for a payment that did not happen");
        db.debitWithEvent("A", BigDecimal.TEN, false);
        assertEquals(new BigDecimal("90"), db.balance("A"));
        assertEquals(1, db.outboxSize());
        assertThrows(IllegalStateException.class, () -> db.debitWithEvent("A", new BigDecimal("1000"), false));
        assertEquals(1, db.outboxSize());
    }

    @Test
    void crashAfterPublishCausesRedeliveryWhichConsumerDeduplicates() {
        Database db = new Database();
        db.open("A", new BigDecimal("100"));
        db.debitWithEvent("A", new BigDecimal("10"), false);
        db.debitWithEvent("A", new BigDecimal("20"), false);

        IdempotentConsumer consumer = new IdempotentConsumer();
        List<Long> deliveries = new ArrayList<>();
        Relay relay = new Relay(db, e -> {
            deliveries.add(e.eventId());
            consumer.accept(e);
        });

        assertThrows(IllegalStateException.class, () -> relay.relayOnce(10, true)); // published #1, crashed before mark
        assertEquals(2, relay.relayOnce(10, false));                                 // #1 again, then #2
        assertEquals(0, relay.relayOnce(10, false), "nothing left");

        assertEquals(List.of(1L, 1L, 2L), deliveries, "at-least-once: event 1 delivered twice, in order");
        assertEquals(1, consumer.duplicatesIgnored());
        assertEquals(new BigDecimal("30"), consumer.spent("A"), "effect applied exactly once per event");
    }

    @Test
    void brokerOutageLosesNothing() {
        Database db = new Database();
        db.open("A", new BigDecimal("100"));
        db.debitWithEvent("A", BigDecimal.ONE, false);
        IdempotentConsumer consumer = new IdempotentConsumer();
        boolean[] brokerUp = {false};
        Relay relay = new Relay(db, e -> {
            if (!brokerUp[0]) throw new IllegalStateException("broker unavailable");
            consumer.accept(e);
        });
        assertThrows(IllegalStateException.class, () -> relay.relayOnce(10, false));
        brokerUp[0] = true;
        assertEquals(1, relay.relayOnce(10, false));
        assertEquals(BigDecimal.ONE, consumer.spent("A"));
    }
}
