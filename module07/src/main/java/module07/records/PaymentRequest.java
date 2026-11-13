package module07.records;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Nested records, defensive copies, Optional component handled via overload, and a local record in a method. */
public record PaymentRequest(String id, Money amount, Counterparty from, Counterparty to, List<String> tags, String reference) {

    public record Counterparty(String accountId, String name) {
        public Counterparty {
            Objects.requireNonNull(accountId);
            Objects.requireNonNull(name);
        }
    }

    public PaymentRequest {
        Objects.requireNonNull(id);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        tags = List.copyOf(tags);
        if (from.accountId().equals(to.accountId())) throw new IllegalArgumentException("same account");
        if (amount.isNegative() || amount.amount().signum() == 0) throw new IllegalArgumentException("amount must be positive");
    }

    public PaymentRequest(String id, Money amount, Counterparty from, Counterparty to) {
        this(id, amount, from, to, List.of(), null);
    }

    /** An accessor must return the component type exactly, so the Optional view gets a different name. */
    public Optional<String> optionalReference() {
        return Optional.ofNullable(reference);
    }

    /** Local record scoped to the computation: ideal for intermediate shapes in streams. */
    public static String describeAll(List<PaymentRequest> requests) {
        record Line(String id, String text) {
        }
        return requests.stream()
                .map(r -> new Line(r.id(), r.amount() + " " + r.from().name() + " -> " + r.to().name()))
                .map(l -> l.id() + ": " + l.text())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}
