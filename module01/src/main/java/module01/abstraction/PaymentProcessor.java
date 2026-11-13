package module01.abstraction;

import java.math.BigDecimal;

/**
 * Abstraction: callers depend on "what" (process a payment) not "how" (card network, bank rails).
 * The abstract class owns the invariant workflow; subclasses supply the varying step.
 */
public abstract class PaymentProcessor {

    /** Template method: validation and auditing are fixed, settlement varies. */
    public final PaymentReceipt process(String paymentId, BigDecimal amount, String currency) {
        validate(paymentId, amount, currency);
        String reference = settle(amount, currency);
        return new PaymentReceipt(paymentId, amount, currency, rail(), reference);
    }

    protected abstract String settle(BigDecimal amount, String currency);

    protected abstract String rail();

    private static void validate(String paymentId, BigDecimal amount, String currency) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("paymentId must not be blank");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("currency must be an ISO-4217 code");
        }
    }
}
