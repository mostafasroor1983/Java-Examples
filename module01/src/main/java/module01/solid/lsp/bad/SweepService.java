package module01.solid.lsp.bad;

import java.math.BigDecimal;
import java.util.List;

/** Generic code written against the base contract; it is not wrong, the hierarchy is. */
public class SweepService {
    public BigDecimal sweep(List<Account> accounts, BigDecimal amountEach) {
        BigDecimal total = BigDecimal.ZERO;
        for (Account account : accounts) {
            account.withdraw(amountEach); // blows up on FixedDepositAccount
            total = total.add(amountEach);
        }
        return total;
    }
}
