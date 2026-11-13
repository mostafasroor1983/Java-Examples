package com.revolut.interview.payment;

import com.revolut.interview.bank.Money;

import java.time.Instant;
import java.util.Objects;

/** Immutable payment snapshot; transitions produce a new instance. */
public record Payment(String id, String customerId, String merchantId, Money amount, PaymentStatus status, String reason, Instant updatedAt) {
    public Payment {
        Objects.requireNonNull(id);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(merchantId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(status);
        Objects.requireNonNull(updatedAt);
        if (!amount.isPositive()) throw new IllegalArgumentException("amount must be positive");
    }

    public Payment with(PaymentStatus next, String why, Instant at) {
        return new Payment(id, customerId, merchantId, amount, next, why, at);
    }
}
