package module08.hierarchy;

/**
 * An Error, not an Exception: the ledger invariant (debits == credits) is broken.
 * Nothing in the request path should catch this; let the process fail fast and page someone.
 */
public final class LedgerCorruptedError extends Error {
    public LedgerCorruptedError(String message) {
        super(message);
    }
}
