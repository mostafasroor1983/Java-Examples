package module01.solid.isp.bad;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RefundOnlyGateway implements PaymentGateway {
    private final List<String> refunds = new ArrayList<>();

    @Override
    public String charge(String card, BigDecimal amount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refund(String reference, BigDecimal amount) {
        refunds.add(reference + ":" + amount);
    }

    @Override
    public void settleBatch() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String generateMonthlyReport() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rotateApiKeys() {
        throw new UnsupportedOperationException();
    }

    public List<String> refunds() {
        return refunds;
    }
}
