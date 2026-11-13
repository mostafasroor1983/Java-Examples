package module01.abstraction;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

public final class CardPaymentProcessor extends PaymentProcessor {
    private final AtomicLong sequence = new AtomicLong();

    @Override
    protected String settle(BigDecimal amount, String currency) {
        return "CARD-" + sequence.incrementAndGet();
    }

    @Override
    protected String rail() {
        return "CARD";
    }
}
