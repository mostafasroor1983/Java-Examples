package module05.repository;

import java.util.Objects;

public record PaymentId(String value) {
    public PaymentId {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException("blank id");
    }
}
