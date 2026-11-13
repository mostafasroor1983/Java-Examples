package module08.hierarchy;

import java.math.BigDecimal;

public final class InsufficientFundsException extends PaymentException {
    private final BigDecimal available;
    private final BigDecimal requested;

    public InsufficientFundsException(String accountId, BigDecimal available, BigDecimal requested) {
        super("INSUFFICIENT_FUNDS", "account " + accountId + ": available " + available + ", requested " + requested);
        this.available = available;
        this.requested = requested;
    }

    public BigDecimal available() { return available; }
    public BigDecimal requested() { return requested; }

    @Override
    public boolean retryable() {
        return false;
    }
}
