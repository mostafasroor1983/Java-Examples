package module01.hexagonal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter for tests and demos. A single monitor stands in for a DB transaction: correct inside ONE JVM only.
 * It gives no durability and no guarantee across processes; a real adapter relies on the database.
 */
public final class InMemoryAccountRepository implements AccountRepository {
    private final Map<String, Account> accounts = new HashMap<>();
    private final Set<String> processedKeys = new HashSet<>();

    public synchronized void seed(Account account) {
        accounts.put(account.id(), account);
    }

    @Override
    public synchronized Optional<Account> findById(String id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public synchronized boolean isProcessed(String idempotencyKey) {
        return processedKeys.contains(idempotencyKey);
    }

    @Override
    public synchronized void commit(List<Account> updated, String idempotencyKey) {
        if (processedKeys.contains(idempotencyKey)) throw new DuplicateCommandException(idempotencyKey);
        for (Account a : updated) { // validate everything first: all-or-nothing
            Account stored = accounts.get(a.id());
            if (stored == null || stored.version() != a.version() - 1) throw new OptimisticLockException(a.id());
        }
        updated.forEach(a -> accounts.put(a.id(), a));
        processedKeys.add(idempotencyKey);
    }
}
