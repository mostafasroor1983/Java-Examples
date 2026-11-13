package module02.exercise1;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Same ledger, correct key: idempotent record + reliable find. */
public class PaymentLedgerFixed {
    private final Map<PaymentRefFixed, BigDecimal> entries = new HashMap<>();

    /** Idempotent: re-recording the same reference does not double-count. */
    public boolean record(PaymentRefFixed ref, BigDecimal amount) {
        return entries.putIfAbsent(ref, amount) == null;
    }

    public Optional<BigDecimal> find(PaymentRefFixed ref) {
        return Optional.ofNullable(entries.get(ref));
    }

    public BigDecimal total() {
        return entries.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int size() {
        return entries.size();
    }
}
