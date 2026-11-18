package module01.hexagonal;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port owned by the application layer. A JDBC/JPA adapter would implement {@link #commit} as a single
 * DB transaction: {@code UPDATE account SET balance=?, version=? WHERE id=? AND version=?} for each account plus
 * {@code INSERT INTO processed_command(key)} guarded by a unique constraint.
 */
public interface AccountRepository {
    Optional<Account> findById(String id);

    boolean isProcessed(String idempotencyKey);

    /**
     * Atomically persists all accounts (each must still be at {@code version - 1} in storage) and records the
     * idempotency key. All-or-nothing.
     *
     * @throws OptimisticLockException  another transaction changed one of the accounts
     * @throws DuplicateCommandException the key was already committed by a concurrent request
     */
    void commit(List<Account> updated, String idempotencyKey);

    final class OptimisticLockException extends RuntimeException {
        public OptimisticLockException(String id) {
            super("concurrent modification of " + id);
        }
    }

    final class DuplicateCommandException extends RuntimeException {
        public DuplicateCommandException(String key) {
            super("command already processed: " + key);
        }
    }
}
