package module01.polymorphism;

import java.math.BigDecimal;

public record BankAccount(String iban) implements PaymentMethod {
    private static final BigDecimal FLAT = new BigDecimal("0.20");

    @Override
    public BigDecimal fee(BigDecimal amount) {
        return FLAT;
    }

    @Override
    public String describe() {
        return "bank account " + iban;
    }
}
