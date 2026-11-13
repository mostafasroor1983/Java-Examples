package module01.solid.dip.good;

import java.math.BigDecimal;
import java.util.Objects;

/** GOOD: depends on the Ledger abstraction, injected by the caller. */
public final class TransferService {
    private final Ledger ledger;

    public TransferService(Ledger ledger) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
    }

    public void transfer(String from, String to, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("from and to must differ");
        }
        ledger.record(from, to, amount);
    }
}
