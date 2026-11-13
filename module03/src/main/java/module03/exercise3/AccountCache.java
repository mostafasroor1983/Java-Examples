package module03.exercise3;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Exercise 3 v1: naive unbounded cache. Correct for a single thread, but grows forever.
 */
public class AccountCache {
    public record Account(String id, BigDecimal balance) {
    }

    private final Map<String, Account> cache = new HashMap<>();

    public void put(Account account) {
        cache.put(account.id(), account);
    }

    public Optional<Account> get(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    public int size() {
        return cache.size();
    }
}
