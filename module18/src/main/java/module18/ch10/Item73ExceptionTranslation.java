package module18.ch10;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

/**
 * Item 73 - Throw exceptions appropriate to the abstraction.
 *
 * <p>A ledger service should not leak {@link SQLException} to its callers: they cannot handle it meaningfully and it
 * ties them to JDBC. Translate at the boundary to a domain exception, and CHAIN the cause so the root cause and stack
 * trace survive for debugging. Classify while translating (transient vs permanent) so callers know whether to retry.
 */
public final class Item73ExceptionTranslation {
    private Item73ExceptionTranslation() {
    }

    public static final class LedgerUnavailableException extends RuntimeException {
        @java.io.Serial
        private static final long serialVersionUID = 1L; // Item 86: always explicit

        private final boolean retryable;

        public LedgerUnavailableException(String message, Throwable cause, boolean retryable) {
            super(message, cause);
            this.retryable = retryable;
        }

        public boolean retryable() {
            return retryable;
        }
    }

    /** Low-level layer. */
    interface LedgerDao {
        long balanceCents(String account) throws SQLException;
    }

    public static long balance(String account, boolean failTransiently, boolean failPermanently) {
        LedgerDao dao = acc -> {
            if (failTransiently) throw new SQLTransientConnectionException("connection reset");
            if (failPermanently) throw new SQLException("relation \"ledger\" does not exist");
            return 12_345;
        };
        try {
            return dao.balanceCents(account);
        } catch (SQLTransientConnectionException e) {
            throw new LedgerUnavailableException("ledger temporarily unavailable", e, true);
        } catch (SQLException e) {
            throw new LedgerUnavailableException("ledger query failed", e, false);
        }
    }
}
