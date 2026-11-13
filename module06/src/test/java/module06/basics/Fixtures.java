package module06.basics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Fixtures {
    private Fixtures() {
    }

    public static Transaction tx(String id, String acc, String ccy, String amt, Transaction.Category cat, String date) {
        return new Transaction(id, acc, ccy, new BigDecimal(amt), cat, LocalDate.parse(date));
    }

    public static List<Transaction> sample() {
        return List.of(
                tx("t1", "A", "GBP", "2000.00", Transaction.Category.SALARY, "2024-03-01"),
                tx("t2", "A", "GBP", "-45.50", Transaction.Category.GROCERIES, "2024-03-02"),
                tx("t3", "A", "GBP", "-12.00", Transaction.Category.TRANSPORT, "2024-03-02"),
                tx("t4", "B", "EUR", "-300.00", Transaction.Category.TRANSFER, "2024-03-03"),
                tx("t5", "B", "EUR", "-1.99", Transaction.Category.FEES, "2024-03-03"),
                tx("t6", "A", "GBP", "-500.00", Transaction.Category.FX, "2024-03-05"),
                tx("t7", "C", "USD", "100.00", Transaction.Category.TRANSFER, "2024-04-01"));
    }
}
