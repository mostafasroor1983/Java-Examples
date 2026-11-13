package module02.mutablekey;

import java.util.Objects;

/** GOOD: immutable key - a record. The hash cannot drift after insertion. */
public record PaymentKey(String merchantId, String orderId) {
    public PaymentKey {
        Objects.requireNonNull(merchantId, "merchantId");
        Objects.requireNonNull(orderId, "orderId");
    }
}
