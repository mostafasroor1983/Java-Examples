package module08.response;

import module08.hierarchy.InsufficientFundsException;
import module08.hierarchy.InvalidPaymentException;
import module08.hierarchy.PaymentException;
import module08.hierarchy.PaymentProviderException;
import module08.circuitbreaker.CircuitBreaker;

import java.util.Map;
import java.util.Objects;

/**
 * API error body. Maps domain exceptions to HTTP status + stable code + safe message.
 * Internal details (stack traces, SQL, provider payloads) never leak to the client.
 */
public record PaymentErrorResponse(int status, String code, String message, boolean retryable, Map<String, String> details) {

    public PaymentErrorResponse {
        Objects.requireNonNull(code);
        Objects.requireNonNull(message);
        details = Map.copyOf(details);
    }

    public static PaymentErrorResponse from(Throwable t) {
        return switch (t) {
            case InsufficientFundsException e -> new PaymentErrorResponse(402, e.code(), "Insufficient funds", false,
                    Map.of("available", e.available().toPlainString(), "requested", e.requested().toPlainString()));
            case InvalidPaymentException e -> new PaymentErrorResponse(400, e.code(), e.getMessage(), false, Map.of());
            case PaymentProviderException e -> new PaymentErrorResponse(e.retryable() ? 503 : 502, e.code(),
                    "Payment provider unavailable", e.retryable(), Map.of());
            case CircuitBreaker.CircuitOpenException e -> new PaymentErrorResponse(503, "CIRCUIT_OPEN", "Service temporarily unavailable", true, Map.of());
            case PaymentException e -> new PaymentErrorResponse(500, e.code(), "Payment failed", e.retryable(), Map.of());
            case IllegalArgumentException e -> new PaymentErrorResponse(400, "BAD_REQUEST", "Invalid request", false, Map.of());
            default -> new PaymentErrorResponse(500, "INTERNAL_ERROR", "Unexpected error", false, Map.of());
        };
    }
}
