package module08.hierarchy;

/**
 * Root of the domain hierarchy. Unchecked: business code rarely can recover at the call site,
 * so forcing `throws` everywhere adds noise without safety. Carries a stable error code for APIs/logs.
 */
public abstract class PaymentException extends RuntimeException {
    private final String code;

    protected PaymentException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected PaymentException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** Whether a client may retry the same request unchanged. */
    public abstract boolean retryable();
}
