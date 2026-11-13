package module08.hierarchy;

public final class InvalidPaymentException extends PaymentException {
    public InvalidPaymentException(String message) {
        super("INVALID_PAYMENT", message);
    }

    @Override
    public boolean retryable() {
        return false;
    }
}
