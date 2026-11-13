package module01.solid.dip.bad;

import java.math.BigDecimal;

/**
 * BAD: high-level policy (transfers) depends on a low-level detail (MySQL) and constructs it.
 * Impossible to unit-test without MySQL, impossible to swap the store.
 */
public class TransferService {
    private final MySqlLedger ledger = new MySqlLedger();

    public void transfer(String from, String to, BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        ledger.insert(from, to, amount);
    }

    public MySqlLedger ledger() {
        return ledger;
    }
}
