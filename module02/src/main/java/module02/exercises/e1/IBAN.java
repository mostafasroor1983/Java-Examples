package module02.exercises.e1;

import java.util.Objects;

public final class IBAN {
    public static final int LENGTH = 21;
    private final String value;

    public IBAN(String value) {
        Objects.requireNonNull(value, "iban can't be null.");
        this.value = value.trim().replaceAll("\\s+", "").toUpperCase();
        if (this.value.length() != LENGTH) {
            throw new IllegalArgumentException("iban should have " + LENGTH + " length.");
        }

        if (!this.value.substring(0, 2).equals("AE") || !this.value.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}")) {
            throw new IllegalArgumentException("iban is wrong.");
        }
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IBAN iban = (IBAN) o;
        return value.equals(iban.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
