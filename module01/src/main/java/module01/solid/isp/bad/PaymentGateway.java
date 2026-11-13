package module01.solid.isp.bad;

import java.math.BigDecimal;

/** BAD: fat interface. A refund-only client is forced to depend on (and stub) everything. */
public interface PaymentGateway {
    String charge(String card, BigDecimal amount);

    void refund(String reference, BigDecimal amount);

    void settleBatch();

    String generateMonthlyReport();

    void rotateApiKeys();
}
