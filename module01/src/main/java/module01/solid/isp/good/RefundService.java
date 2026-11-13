package module01.solid.isp.good;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** GOOD: implements only what it can honour. */
public final class RefundService implements Refunder {
    private final List<String> refunds = new ArrayList<>();

    @Override
    public void refund(String reference, BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("refund must be positive");
        }
        refunds.add(reference + ":" + amount);
    }

    public List<String> refunds() {
        return List.copyOf(refunds);
    }
}
