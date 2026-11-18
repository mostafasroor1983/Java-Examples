package module18.ch10;

import java.math.BigDecimal;

/**
 * Item 75 - Include failure-capture information in detail messages.
 *
 * <p>When a failure happens in production, the exception is often all you have. Include the values of every parameter
 * and field that contributed (account id, requested amount, limit, state), and expose them through accessors so
 * code can act on them. Fintech caveat: NEVER include secrets (passwords, PINs, full PANs, tokens) or more personal
 * data than needed, because messages land in logs and monitoring tools. Record the identifiers, not the secrets.
 */
public final class Item75FailureCaptureInfo {
    private Item75FailureCaptureInfo() {
    }

    public static final class LimitExceededException extends RuntimeException {
        @java.io.Serial
        private static final long serialVersionUID = 1L; // Item 86: always explicit

        private final String accountId;
        private final BigDecimal requested;
        private final BigDecimal remainingLimit;

        public LimitExceededException(String accountId, BigDecimal requested, BigDecimal remainingLimit) {
            super("limit exceeded: account=" + accountId + ", requested=" + requested.toPlainString()
                    + ", remainingLimit=" + remainingLimit.toPlainString());
            this.accountId = accountId;
            this.requested = requested;
            this.remainingLimit = remainingLimit;
        }

        public String accountId() { return accountId; }

        public BigDecimal requested() { return requested; }

        public BigDecimal remainingLimit() { return remainingLimit; }
    }
}
