package module02.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Typed ID: prevents passing a wallet id where a customer id is expected. */
public record CustomerId(UUID value) {
    public CustomerId {
        Objects.requireNonNull(value, "value");
    }

    public static CustomerId random() {
        return new CustomerId(UUID.randomUUID());
    }

    public static CustomerId parse(String s) {
        return new CustomerId(UUID.fromString(s));
    }
}
