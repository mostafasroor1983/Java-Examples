package module15.p16outbox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * P16 - Transactional outbox + idempotent consumer: how "exactly once" is really achieved.
 *
 * <p>The dual-write problem: "update balance in DB, then publish PaymentCompleted to Kafka". If the process dies between
 * the two, the DB says paid but nobody is notified; reverse the order and you announce a payment that rolled back.
 *
 * <p>Fix:
 * <ol>
 *   <li><b>Outbox</b>: write the event into an {@code outbox} table in the SAME DB transaction as the balance change.
 *   Either both exist or neither does.</li>
 *   <li><b>Relay</b>: a separate loop reads unpublished outbox rows in order, publishes them, then marks them published.
 *   A crash after publish but before marking means the row is published AGAIN: <b>at-least-once</b>.</li>
 *   <li><b>Idempotent consumer</b>: records processed event ids in the same transaction as its own side effect and
 *   ignores repeats. At-least-once delivery plus idempotent processing gives an effectively-once effect.</li>
 * </ol>
 * The "database" here is a lock-protected in-memory simulation so the protocol can be tested with failure injection.
 * In production the outbox is a real table (often relayed by CDC such as Debezium) and the broker is Kafka or similar.
 */
public final class P16TransactionalOutbox {

    public record Event(long eventId, String type, String accountId, BigDecimal amount) {
    }

    /** Simulated relational DB: one lock = one serialisable transaction. */
    public static final class Database {
        private final Map<String, BigDecimal> balances = new HashMap<>();
        private final LinkedHashMap<Long, Event> outbox = new LinkedHashMap<>(); // insertion = commit order
        private final Set<Long> published = new HashSet<>();
        private long nextEventId = 1;

        public synchronized void open(String account, BigDecimal opening) {
            balances.put(account, opening);
        }

        /** Debit + outbox insert in ONE transaction. A failure inside rolls back both. */
        public synchronized Event debitWithEvent(String account, BigDecimal amount, boolean crashBeforeCommit) {
            BigDecimal current = balances.get(account);
            if (current == null) throw new IllegalArgumentException("unknown account");
            if (current.compareTo(amount) < 0) throw new IllegalStateException("insufficient funds");
            Event e = new Event(nextEventId, "PaymentCompleted", account, amount);
            if (crashBeforeCommit) throw new IllegalStateException("simulated crash: transaction rolled back");
            balances.put(account, current.subtract(amount)); // both writes become visible together
            outbox.put(nextEventId++, e);
            return e;
        }

        synchronized List<Event> unpublished(int limit) {
            List<Event> out = new ArrayList<>();
            for (Event e : outbox.values()) {
                if (!published.contains(e.eventId())) out.add(e);
                if (out.size() == limit) break;
            }
            return out;
        }

        synchronized void markPublished(long eventId) {
            published.add(eventId);
        }

        public synchronized BigDecimal balance(String account) {
            return balances.get(account);
        }

        public synchronized int outboxSize() {
            return outbox.size();
        }
    }

    /** Relay: at-least-once. {@code crashAfterPublish} injects the failure that causes redelivery. */
    public static final class Relay {
        private final Database db;
        private final Consumer<Event> broker;

        public Relay(Database db, Consumer<Event> broker) {
            this.db = Objects.requireNonNull(db);
            this.broker = Objects.requireNonNull(broker);
        }

        /** @return number of events published in this pass */
        public int relayOnce(int batchSize, boolean crashAfterPublish) {
            int n = 0;
            for (Event e : db.unpublished(batchSize)) {
                broker.accept(e);  // if this throws, the row stays unpublished and is retried next pass
                n++;
                if (crashAfterPublish) throw new IllegalStateException("simulated crash after publish, before mark");
                db.markPublished(e.eventId());
            }
            return n;
        }
    }

    /** Consumer side (e.g. notification or loyalty-points service) with its own store and dedup table. */
    public static final class IdempotentConsumer implements Consumer<Event> {
        private final Set<Long> processed = new HashSet<>();
        private final Map<String, BigDecimal> spentByAccount = new HashMap<>();
        private int duplicatesIgnored;

        @Override
        public synchronized void accept(Event e) {
            if (!processed.add(e.eventId())) { // dedup + effect in one "transaction" (same lock)
                duplicatesIgnored++;
                return;
            }
            spentByAccount.merge(e.accountId(), e.amount(), BigDecimal::add);
        }

        public synchronized BigDecimal spent(String account) {
            return spentByAccount.getOrDefault(account, BigDecimal.ZERO);
        }

        public synchronized int duplicatesIgnored() {
            return duplicatesIgnored;
        }
    }
}
