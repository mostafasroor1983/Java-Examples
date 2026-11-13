package module01.exercise1;

import java.util.Objects;

public record WalletId(String value) {
    public WalletId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("wallet id must not be blank");
        }
    }
}
