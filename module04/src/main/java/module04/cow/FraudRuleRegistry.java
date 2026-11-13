package module04.cow;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * CopyOnWriteArrayList: every write copies the array; reads are lock-free and iterate a snapshot.
 * Perfect for read-mostly listener/rule lists (thousands of reads per second, a handful of updates per day).
 * Terrible for write-heavy workloads (O(n) per write).
 */
public final class FraudRuleRegistry {
    public record Rule(String name, Predicate<BigDecimal> rejects) {
    }

    private final List<Rule> rules = new CopyOnWriteArrayList<>();

    public void register(Rule rule) {
        rules.add(rule);
    }

    public boolean unregister(String name) {
        return rules.removeIf(r -> r.name().equals(name));
    }

    /** Iterates a snapshot: a concurrent register() never causes ConcurrentModificationException. */
    public List<String> evaluate(BigDecimal amount) {
        return rules.stream().filter(r -> r.rejects().test(amount)).map(Rule::name).toList();
    }

    public int size() {
        return rules.size();
    }
}
