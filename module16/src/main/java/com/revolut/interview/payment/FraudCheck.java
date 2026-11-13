package com.revolut.interview.payment;

@FunctionalInterface
public interface FraudCheck {
    /** @return null if clean, otherwise the reason to decline. */
    String evaluate(Payment payment);
}
