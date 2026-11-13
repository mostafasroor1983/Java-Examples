package module08.hierarchy;

/** Infrastructure failure talking to an external provider; usually retryable. Wraps the technical cause. */
public final class PaymentProviderException extends PaymentException {
    private final boolean retryable;

    public PaymentProviderException(String message, Throwable cause, boolean retryable) {
        super("PROVIDER_ERROR", message, cause);
        this.retryable = retryable;
    }

    @Override
    public boolean retryable() {
        return retryable;
    }
}
