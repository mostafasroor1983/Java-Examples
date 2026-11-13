package module02.exercise1;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Ledger keyed by a buggy PaymentRef. Demonstrates the symptom; see PaymentLedgerFixed. */
public class PaymentLedger {
    private final Map<PaymentRef, BigDecimal> entries = new HashMap<>();

    public void record(PaymentRef ref, BigDecimal amount) {
        entries.put(ref, amount);
    }

    public Optional<BigDecimal> find(PaymentRef ref) {
        return Optional.ofNullable(entries.get(ref));
    }

    public int size() {
        return entries.size();
    }
}
