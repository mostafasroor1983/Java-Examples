package com.revolut.buildit.shoppingcart;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared tiny money helper for Build It modules that only need one currency. */
public final class Money {
    private Money() {
    }

    public static BigDecimal of(String v) {
        return new BigDecimal(v).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}
