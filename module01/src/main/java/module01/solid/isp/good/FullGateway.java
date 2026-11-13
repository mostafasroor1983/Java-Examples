package module01.solid.isp.good;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/** A client that needs everything simply implements several small interfaces. */
public final class FullGateway implements Charger, Refunder, Settler {
    private final AtomicInteger charges = new AtomicInteger();
    private final AtomicInteger refunds = new AtomicInteger();
    private volatile boolean settled;

    @Override
    public String charge(String card, BigDecimal amount) {
        return "CH-" + charges.incrementAndGet();
    }

    @Override
    public void refund(String reference, BigDecimal amount) {
        refunds.incrementAndGet();
    }

    @Override
    public void settleBatch() {
        settled = true;
    }

    public boolean settled() {
        return settled;
    }
}
