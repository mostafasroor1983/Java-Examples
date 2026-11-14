package module01.solid.lsp.good;

import java.math.BigDecimal;
import java.util.List;

public final class SweepService {

    // check in compiling time that account is withdrawal using "? extends WithdrawableAccount".
    public BigDecimal sweep(List<? extends WithdrawableAccount> accounts,
                            BigDecimal amountEach) {
        BigDecimal total = BigDecimal.ZERO;
        for (WithdrawableAccount account : accounts) {
            account.withdraw(amountEach);
            total = total.add(amountEach);
        }
        return total;
    }
}
