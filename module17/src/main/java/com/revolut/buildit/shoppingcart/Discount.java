package com.revolut.buildit.shoppingcart;

import java.math.BigDecimal;

/** Pricing rule applied to a cart snapshot; returns the discount amount (>= 0). */
@FunctionalInterface
public interface Discount {
    BigDecimal apply(ShoppingCart.Snapshot cart);

    static Discount percentageOff(String code, BigDecimal pct) {
        return cart -> cart.couponCode().equals(code) ? Money.scale(cart.subtotal().multiply(pct).divide(BigDecimal.valueOf(100))) : BigDecimal.ZERO;
    }

    /** Buy N of a SKU, get 1 free. */
    static Discount buyNGetOneFree(String sku, int n) {
        return cart -> {
            ShoppingCart.Line line = cart.lines().get(sku);
            if (line == null) return BigDecimal.ZERO;
            int free = line.quantity() / (n + 1);
            return Money.scale(line.unitPrice().multiply(BigDecimal.valueOf(free)));
        };
    }

    static Discount freeShippingOver(BigDecimal threshold, BigDecimal shipping) {
        return cart -> cart.subtotal().compareTo(threshold) >= 0 ? shipping : BigDecimal.ZERO;
    }
}
