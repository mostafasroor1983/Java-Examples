package module01.solid.lsp.good;

import java.math.BigDecimal;

/** GOOD: does not pretend to be withdrawable; the type system prevents misuse. */
public final class FixedDepositAccount implements ReadableAccount {
    private final BigDecimal balance;

    public FixedDepositAccount(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public BigDecimal balance() {
        return balance;
    }
}
