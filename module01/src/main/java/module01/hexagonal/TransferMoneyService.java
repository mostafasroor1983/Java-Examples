package module01.hexagonal;

import java.util.List;
import java.util.Objects;

/**
 * Application service (use case) in a hexagonal / ports-and-adapters layout.
 *
 * <pre>
 *  [REST / gRPC / Kafka adapter] -> TransferMoneyService -> AccountRepository (port) -> [JDBC / in-memory adapter]
 *                                         |
 *                                      Account (pure domain, no framework imports)
 * </pre>
 *
 * Responsibilities: orchestration only. Business rules live in {@link Account}; persistence guarantees
 * (atomicity, version checks, unique idempotency key) live behind the port. On an optimistic-lock conflict
 * the whole read-modify-write is retried a bounded number of times, then the conflict is surfaced.
 */
public final class TransferMoneyService {
    public enum Outcome { COMPLETED, DUPLICATE }

    private final AccountRepository repository;
    private final int maxAttempts;

    public TransferMoneyService(AccountRepository repository, int maxAttempts) {
        this.repository = Objects.requireNonNull(repository, "repository");
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        this.maxAttempts = maxAttempts;
    }

    public Outcome transfer(TransferCommand cmd) {
        for (int attempt = 1; ; attempt++) {
            if (repository.isProcessed(cmd.idempotencyKey())) return Outcome.DUPLICATE; // fast path for retries
            Account from = load(cmd.fromAccount());
            Account to = load(cmd.toAccount());
            Account debited = from.debit(cmd.amount());   // domain rule: may throw InsufficientFunds (not retried)
            Account credited = to.credit(cmd.amount());
            try {
                repository.commit(List.of(debited, credited), cmd.idempotencyKey());
                return Outcome.COMPLETED;
            } catch (AccountRepository.DuplicateCommandException e) {
                return Outcome.DUPLICATE; // a concurrent retry of the same request won the race
            } catch (AccountRepository.OptimisticLockException e) {
                if (attempt >= maxAttempts) throw e; // bounded: never spin forever on a hot account
            }
        }
    }

    private Account load(String id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("unknown account " + id));
    }
}
