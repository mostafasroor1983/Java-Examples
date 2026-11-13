package com.revolut.interview.bank;

import java.util.Objects;

public record AccountId(String value) {
    public AccountId {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException("account id must not be blank");
    }
}
