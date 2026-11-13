package module01.solid.lsp.bad;

import java.math.BigDecimal;

public class Account {
    protected BigDecimal balance;

    public Account(BigDecimal balance) {
        this.balance = balance;
    }

    /** Contract: balance decreases by amount; throws only if insufficient funds. */
    public void withdraw(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient funds");
        }
        balance = balance.subtract(amount);
    }

    public BigDecimal balance() {
        return balance;
    }
}
