package module02.valueobjects;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object with normalisation: "Bob@Example.COM" equals "bob@example.com".
 */
public final class Email {
    private static final Pattern SIMPLE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String raw) {
        Objects.requireNonNull(raw, "email");
        String normalised = raw.trim().toLowerCase(Locale.ROOT);
        if (normalised.length() > 254 || !SIMPLE.matcher(normalised).matches()) {
            throw new IllegalArgumentException("invalid email");
        }
        return new Email(normalised);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Email email) && value.equals(email.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
