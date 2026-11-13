package module01.solid.srp.good;

import java.math.BigDecimal;

public interface Notifier {
    void paymentReceived(String customerEmail, BigDecimal amount);
}
