package module01.ddd;

/** Raised when a user tries to act as both maker and checker, or approves the same payment twice. */
public final class SeparationOfDutiesException extends RuntimeException {
    public SeparationOfDutiesException(String message) {
        super(message);
    }
}
