package module01.polymorphism;

import java.math.BigDecimal;

/**
 * Two flavours of polymorphism: subtype dispatch (method.fee) and
 * pattern matching over a sealed type (exhaustive switch, no default).
 */
public final class FeeCalculator {
    private FeeCalculator() {
    }

    /** Runtime dispatch - the classic OOP way. */
    public static BigDecimal feeViaDispatch(PaymentMethod method, BigDecimal amount) {
        return method.fee(amount);
    }

    /** Pattern matching - useful when behaviour belongs to the use case, not the type. */
    public static String riskBand(PaymentMethod method, BigDecimal amount) {
        return switch (method) {
            case Card c when amount.compareTo(new BigDecimal("1000")) > 0 -> "HIGH";
            case Card c -> "MEDIUM";
            case BankAccount b -> "LOW";
            case Wallet w -> "LOW";
        };
    }
}
