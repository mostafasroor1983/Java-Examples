package module01.ddd;

import java.util.Objects;

/** Identity of an operator (maker or checker). Validated at construction so it can never be blank. */
public record UserId(String value) {
    public UserId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("user id must not be blank");
    }
}
