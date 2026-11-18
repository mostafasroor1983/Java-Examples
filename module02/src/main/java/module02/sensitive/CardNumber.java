package module02.sensitive;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Primary Account Number (PAN) value object.
 *
 * <p>Trap: a record's generated {@code toString()} prints every component. {@code log.info("charging {}", card)}
 * would write the full PAN to logs, a PCI-DSS violation. This record overrides toString to mask it, and
 * exposes only {@link #last4()} / {@link #masked()} for display.
 *
 * <p>Validation happens once, at construction: digits only, 12..19 long, Luhn checksum valid.
 * Equality is by value (two instances with the same PAN are equal), so it is safe as a map key.
 *
 * <p>Production note: in a real system the PAN never reaches most services. It is tokenised at the edge
 * (a vault / HSM returns a token), and only the PCI-scoped vault holds the clear value.
 */
public record CardNumber(String pan) {
    public CardNumber {
        Objects.requireNonNull(pan, "pan");
        pan = pan.replace(" ", "");
        if (!pan.matches("\\d{12,19}")) throw new IllegalArgumentException("PAN must be 12-19 digits");
        if (!luhnValid(pan)) throw new IllegalArgumentException("PAN fails Luhn check"); // value NOT echoed
    }

    public String last4() {
        return pan.substring(pan.length() - 4);
    }

    public String masked() {
        return "*".repeat(pan.length() - 4) + last4();
    }

    /** Masked on purpose: records print all components by default. */
    @Override
    public String toString() {
        return "CardNumber[" + masked() + "]";
    }

    /**
     * Constant-time comparison of a candidate PAN, avoiding early-exit timing leaks when matching
     * against a stored value (illustrative; real systems compare tokens or keyed hashes, not clear PANs).
     */
    public boolean matches(String candidate) {
        if (candidate == null) return false;
        return MessageDigest.isEqual(pan.getBytes(StandardCharsets.US_ASCII),
                candidate.replace(" ", "").getBytes(StandardCharsets.US_ASCII));
    }

    static boolean luhnValid(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }
}
