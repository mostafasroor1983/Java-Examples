package module01.abstraction;

import java.math.BigDecimal;

public record PaymentReceipt(String paymentId, BigDecimal amount, String currency,
                             String rail, String reference) {
}
