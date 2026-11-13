package module01.polymorphism;

import java.math.BigDecimal;

public record Wallet(String walletId) implements PaymentMethod {
    @Override
    public BigDecimal fee(BigDecimal amount) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public String describe() {
        return "wallet " + walletId;
    }
}
