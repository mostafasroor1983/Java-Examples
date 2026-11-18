package module18.ch10;

import java.math.BigDecimal;

/**
 * Item 70 - Use checked exceptions for recoverable conditions and runtime exceptions for programming errors.
 *
 * <p>Checked exception: the caller can reasonably recover and the API wants to force it to think about it. A card
 * decline carries a decline code and a retry-after hint the caller can act on. Runtime exception: precondition
 * violations ({@code IllegalArgumentException} for a negative amount): the caller has a bug. Never subclass
 * {@code Error}. Give checked exceptions accessor methods for the recovery information, not just a message.
 */
public final class Item70CheckedVsRuntime {
    private Item70CheckedVsRuntime() {
    }

    public static final class CardDeclinedException extends Exception {
        @java.io.Serial
        private static final long serialVersionUID = 1L; // Item 86: always explicit

        private final String declineCode;
        private final boolean retryable;

        public CardDeclinedException(String declineCode, boolean retryable) {
            super("card declined: " + declineCode);
            this.declineCode = declineCode;
            this.retryable = retryable;
        }

        public String declineCode() {
            return declineCode;
        }

        public boolean retryable() {
            return retryable;
        }
    }

    /**
     * @throws CardDeclinedException    when the issuer declines (recoverable: ask for another card or retry)
     * @throws IllegalArgumentException when amount is not positive (caller bug)
     */
    public static String authorise(BigDecimal amount, String issuerResponse) throws CardDeclinedException {
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        return switch (issuerResponse) {
            case "00" -> "AUTH-OK";
            case "51" -> throw new CardDeclinedException("INSUFFICIENT_FUNDS", false);
            case "91" -> throw new CardDeclinedException("ISSUER_UNAVAILABLE", true);
            default -> throw new CardDeclinedException("DO_NOT_HONOUR", false);
        };
    }
}
