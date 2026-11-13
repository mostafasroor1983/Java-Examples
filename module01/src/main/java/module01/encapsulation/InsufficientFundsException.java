package module01.encapsulation;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String accountId, BigDecimal balance, BigDecimal requested) {
        super("account " + accountId + " has " + balance + ", requested " + requested);
    }
}
