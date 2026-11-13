package module01.solid.lsp.bad;

import java.math.BigDecimal;

/**
 * BAD: a subtype that strengthens preconditions (no withdrawals at all).
 * Code written against Account breaks when handed a FixedDepositAccount.
 */
public class FixedDepositAccount extends Account {
    public FixedDepositAccount(BigDecimal balance) {
        super(balance);
    }

    @Override
    public void withdraw(BigDecimal amount) {
        throw new UnsupportedOperationException("fixed deposits cannot be withdrawn");
    }
}
