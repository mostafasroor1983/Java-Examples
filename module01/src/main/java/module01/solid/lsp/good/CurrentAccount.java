package module01.solid.lsp.good;

import java.math.BigDecimal;

public final class CurrentAccount implements WithdrawableAccount {
    private BigDecimal balance;

    public CurrentAccount(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public void withdraw(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient funds");
        }
        balance = balance.subtract(amount);
    }

    @Override
    public BigDecimal balance() {
        return balance;
    }
}
