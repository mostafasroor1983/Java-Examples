package module02.exercise1;

import java.util.Objects;

/** FIXED key: record provides consistent equals/hashCode. */
public record PaymentRefFixed(String merchantId, String orderId) {
    public PaymentRefFixed {
        Objects.requireNonNull(merchantId);
        Objects.requireNonNull(orderId);
    }
}
