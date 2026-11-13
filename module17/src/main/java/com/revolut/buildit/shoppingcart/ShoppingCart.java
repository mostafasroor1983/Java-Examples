package com.revolut.buildit.shoppingcart;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Build It #2: cart with add/remove/update quantity, subtotal, composable discounts, totals never negative.
 * Thread-safe per cart (one user), discount rules are pure functions over an immutable snapshot.
 */
public final class ShoppingCart {
    public record Line(String sku, String name, BigDecimal unitPrice, int quantity) {
        public BigDecimal total() {
            return Money.scale(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }
    }

    public record Snapshot(Map<String, Line> lines, String couponCode, BigDecimal subtotal) {
    }

    public record Totals(BigDecimal subtotal, BigDecimal discount, BigDecimal shipping, BigDecimal total) {
    }

    private final Map<String, Line> lines = new LinkedHashMap<>();
    private final List<Discount> discounts;
    private final BigDecimal shippingFee;
    private String couponCode = "";

    public ShoppingCart(List<Discount> discounts, BigDecimal shippingFee) {
        this.discounts = List.copyOf(discounts);
        this.shippingFee = Money.scale(shippingFee);
    }

    public synchronized void add(String sku, String name, BigDecimal unitPrice, int quantity) {
        requireQuantity(quantity);
        if (unitPrice == null || unitPrice.signum() < 0) throw new IllegalArgumentException("unit price must be >= 0");
        lines.merge(sku, new Line(sku, name, Money.scale(unitPrice), quantity),
                (old, add) -> new Line(sku, old.name(), old.unitPrice(), old.quantity() + add.quantity()));
    }

    public synchronized void updateQuantity(String sku, int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("quantity must be >= 0");
        Line line = lines.get(sku);
        if (line == null) throw new IllegalArgumentException("not in cart: " + sku);
        if (quantity == 0) lines.remove(sku);
        else lines.put(sku, new Line(sku, line.name(), line.unitPrice(), quantity));
    }

    public synchronized boolean remove(String sku) {
        return lines.remove(sku) != null;
    }

    public synchronized void applyCoupon(String code) {
        this.couponCode = Objects.requireNonNullElse(code, "");
    }

    public synchronized Snapshot snapshot() {
        BigDecimal subtotal = lines.values().stream().map(Line::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Snapshot(Map.copyOf(lines), couponCode, Money.scale(subtotal));
    }

    public synchronized Totals totals() {
        Snapshot snap = snapshot();
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal shipping = lines.isEmpty() ? Money.scale(BigDecimal.ZERO) : shippingFee;
        for (Discount d : discounts) {
            BigDecimal amount = Money.scale(d.apply(snap));
            if (amount.signum() < 0) throw new IllegalStateException("discount must not be negative");
            discount = discount.add(amount);
        }
        // Discounts cannot exceed subtotal + shipping.
        BigDecimal cap = snap.subtotal().add(shipping);
        if (discount.compareTo(cap) > 0) discount = cap;
        BigDecimal total = cap.subtract(discount);
        return new Totals(snap.subtotal(), Money.scale(discount), shipping, Money.scale(total));
    }

    public synchronized int itemCount() {
        return lines.values().stream().mapToInt(Line::quantity).sum();
    }

    private static void requireQuantity(int q) {
        if (q <= 0) throw new IllegalArgumentException("quantity must be positive");
    }
}
