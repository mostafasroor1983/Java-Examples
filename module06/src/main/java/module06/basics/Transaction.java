package module06.basics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Transaction(String id, String accountId, String currency, BigDecimal amount, Category category, LocalDate date) {
    public enum Category { GROCERIES, TRANSPORT, TRANSFER, FX, FEES, SALARY }

    public Transaction {
        Objects.requireNonNull(id);
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(category);
        Objects.requireNonNull(date);
    }

    public boolean isDebit() {
        return amount.signum() < 0;
    }
}
