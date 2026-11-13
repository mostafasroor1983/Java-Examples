package module01.solid.dip.good;

import java.math.BigDecimal;

/** Abstraction owned by the high-level module. */
public interface Ledger {
    void record(String from, String to, BigDecimal amount);
}
