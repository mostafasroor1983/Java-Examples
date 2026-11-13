package module04.chm;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BAD: a ConcurrentHashMap does not make compound actions atomic.
 * get + put is a check-then-act race; concurrent credits lose updates.
 */
public final class BrokenBalanceStore {
    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();

    public void open(String id, BigDecimal opening) {
        balances.put(id, opening);
    }

    public void credit(String id, BigDecimal amount) {
        BigDecimal current = balances.get(id);   // read
        balances.put(id, current.add(amount));   // write - another thread may have written in between
    }

    public BigDecimal balance(String id) {
        return balances.get(id);
    }
}
