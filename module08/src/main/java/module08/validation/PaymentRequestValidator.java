package module08.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Accumulating validation for an incoming payment request.
 *
 * <p>Fail-fast (throw on the first problem) is right for internal invariants. At the API boundary it is hostile:
 * the client fixes one field, resubmits, and hits the next error. This validator collects ALL field errors and returns
 * them as data ({@link Result}); exceptions are reserved for unexpected failures.
 *
 * <p>Error messages are stable codes plus a human hint and NEVER echo the submitted value: rejected input is
 * exactly the kind of data (PANs pasted into the reference field, injection payloads) you don't want in logs.
 */
public final class PaymentRequestValidator {
    public record Request(String debtorIban, String creditorIban, BigDecimal amount, String currency, String reference) {
    }

    public record FieldError(String field, String code, String hint) {
    }

    public sealed interface Result {
        record Valid(Request request) implements Result { }

        record Invalid(List<FieldError> errors) implements Result {
            public Invalid {
                errors = List.copyOf(errors);
            }
        }
    }

    private static final Set<String> SUPPORTED = Set.of("EUR", "GBP", "USD");
    private static final BigDecimal MAX_SINGLE_PAYMENT = new BigDecimal("1000000.00");

    public Result validate(Request r) {
        if (r == null) return new Result.Invalid(List.of(new FieldError("request", "REQUIRED", "body is required")));
        List<FieldError> errors = new ArrayList<>();
        checkIban("debtorIban", r.debtorIban(), errors);
        checkIban("creditorIban", r.creditorIban(), errors);
        if (r.debtorIban() != null && normalise(r.debtorIban()).equals(normalise(r.creditorIban() == null ? "" : r.creditorIban()))) {
            errors.add(new FieldError("creditorIban", "SAME_ACCOUNT", "creditor must differ from debtor"));
        }
        if (r.currency() == null || !SUPPORTED.contains(r.currency())) {
            errors.add(new FieldError("currency", "UNSUPPORTED", "one of " + SUPPORTED.stream().sorted().toList()));
        }
        if (r.amount() == null) {
            errors.add(new FieldError("amount", "REQUIRED", "amount is required"));
        } else {
            if (r.amount().signum() <= 0) errors.add(new FieldError("amount", "NOT_POSITIVE", "must be > 0"));
            if (r.amount().stripTrailingZeros().scale() > 2) errors.add(new FieldError("amount", "PRECISION", "max 2 decimals"));
            if (r.amount().compareTo(MAX_SINGLE_PAYMENT) > 0) errors.add(new FieldError("amount", "LIMIT", "exceeds single payment limit"));
        }
        if (r.reference() != null && (r.reference().length() > 140 || !r.reference().matches("[\\p{L}\\p{N} /\\-?:().,'+]*"))) {
            errors.add(new FieldError("reference", "INVALID", "max 140 chars from the SEPA character set"));
        }
        return errors.isEmpty() ? new Result.Valid(r) : new Result.Invalid(errors);
    }

    private static void checkIban(String field, String iban, List<FieldError> errors) {
        if (iban == null || iban.isBlank()) {
            errors.add(new FieldError(field, "REQUIRED", "IBAN is required"));
        } else if (!ibanChecksumValid(normalise(iban))) {
            errors.add(new FieldError(field, "INVALID_IBAN", "format or checksum invalid"));
        }
    }

    private static String normalise(String iban) {
        return iban.replace(" ", "").toUpperCase();
    }

    /** ISO 13616 mod-97 check. */
    static boolean ibanChecksumValid(String iban) {
        if (!iban.matches("[A-Z]{2}\\d{2}[A-Z0-9]{11,30}")) return false;
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        int mod = 0;
        for (char c : rearranged.toCharArray()) {
            int v = Character.isDigit(c) ? c - '0' : c - 'A' + 10;
            mod = (v > 9 ? mod * 100 + v : mod * 10 + v) % 97;
        }
        return mod == 1;
    }
}
