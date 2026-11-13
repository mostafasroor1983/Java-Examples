package module01.abstraction;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

public final class BankTransferProcessor extends PaymentProcessor {
    private final AtomicLong sequence = new AtomicLong();

    @Override
    protected String settle(BigDecimal amount, String currency) {
        return "SEPA-" + sequence.incrementAndGet();
    }

    @Override
    protected String rail() {
        return "BANK_TRANSFER";
    }
}
