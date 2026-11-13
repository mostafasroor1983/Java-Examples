package com.revolut.buildit.shoppingcart;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ShoppingCartTest {
    private ShoppingCart cart() {
        return new ShoppingCart(List.of(
                Discount.percentageOff("SAVE10", BigDecimal.TEN),
                Discount.buyNGetOneFree("coffee", 2),
                Discount.freeShippingOver(Money.of("50"), Money.of("4.99"))), Money.of("4.99"));
    }

    @Test
    void linesMergeUpdateAndRemove() {
        ShoppingCart c = cart();
        c.add("coffee", "Coffee", Money.of("3.00"), 2);
        c.add("coffee", "Coffee", Money.of("3.00"), 1);
        c.add("book", "Book", Money.of("20.00"), 1);
        assertThat(c.itemCount()).isEqualTo(4);
        assertThat(c.snapshot().subtotal()).isEqualTo(Money.of("29.00"));
        c.updateQuantity("book", 2);
        assertThat(c.snapshot().subtotal()).isEqualTo(Money.of("49.00"));
        c.updateQuantity("book", 0);
        assertThat(c.remove("book")).isFalse();
        assertThatThrownBy(() -> c.updateQuantity("ghost", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.add("x", "X", Money.of("1"), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.add("x", "X", Money.of("-1"), 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void discountsComposeAndShippingRules() {
        ShoppingCart c = cart();
        c.add("coffee", "Coffee", Money.of("3.00"), 3); // buy 2 get 1 free -> 3.00 off
        c.add("book", "Book", Money.of("20.00"), 1);
        var t = c.totals();
        assertThat(t.subtotal()).isEqualTo(Money.of("29.00"));
        assertThat(t.shipping()).isEqualTo(Money.of("4.99"));
        assertThat(t.discount()).isEqualTo(Money.of("3.00"));
        assertThat(t.total()).isEqualTo(Money.of("30.99"));

        c.applyCoupon("SAVE10"); // +2.90
        assertThat(c.totals().discount()).isEqualTo(Money.of("5.90"));
        c.add("book", "Book", Money.of("20.00"), 2); // subtotal 69 -> free shipping
        var t2 = c.totals();
        assertThat(t2.subtotal()).isEqualTo(Money.of("69.00"));
        assertThat(t2.discount()).isEqualTo(Money.of("3.00").add(Money.of("6.90")).add(Money.of("4.99")));
        assertThat(t2.total()).isEqualTo(Money.of("69.00").add(Money.of("4.99")).subtract(t2.discount()));
    }

    @Test
    void totalNeverNegativeAndEmptyCartHasNoShipping() {
        ShoppingCart c = new ShoppingCart(List.of(cart -> Money.of("1000")), Money.of("4.99"));
        assertThat(c.totals().total()).isEqualTo(Money.of("0.00"));
        assertThat(c.totals().shipping()).isEqualTo(Money.of("0.00"));
        c.add("pen", "Pen", Money.of("1.50"), 1);
        assertThat(c.totals().total()).isEqualTo(Money.of("0.00"));
        assertThat(c.totals().discount()).isEqualTo(Money.of("6.49"));
    }
}
