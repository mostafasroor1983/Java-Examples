package module07.records;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecordsTest {

    @Test
    void moneyNormalisesScaleAndArithmetic() {
        assertEquals(Money.of("10", "GBP"), Money.of("10.00", "GBP"));
        assertEquals(new BigDecimal("12.50"), Money.of("10", "GBP").plus(Money.of("2.5", "GBP")).amount());
        assertEquals(new BigDecimal("0.15"), Money.of("10", "GBP").percent(new BigDecimal("1.5")).amount());
        assertEquals("10.00 GBP", Money.of("10", "GBP").toString());
        assertThrows(IllegalArgumentException.class, () -> Money.of("1", "GBP").plus(Money.of("1", "EUR")));
        assertThrows(NullPointerException.class, () -> new Money(null, null));
        assertEquals(0, Money.of("100", "JPY").amount().scale());
    }

    @Test
    void paymentRequestValidatesAndCopies() {
        var from = new PaymentRequest.Counterparty("A", "Alice");
        var to = new PaymentRequest.Counterparty("B", "Bob");
        List<String> tags = new ArrayList<>(List.of("x"));
        var req = new PaymentRequest("p1", Money.of("5", "GBP"), from, to, tags, null);
        tags.add("y");
        assertEquals(List.of("x"), req.tags());
        assertTrue(req.optionalReference().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new PaymentRequest("p", Money.of("5", "GBP"), from, from));
        assertThrows(IllegalArgumentException.class, () -> new PaymentRequest("p", Money.zero("GBP"), from, to));
        assertEquals("p1: 5.00 GBP Alice -> Bob", PaymentRequest.describeAll(List.of(req)));
    }

    @Test
    void recordEqualityIsStructural() {
        var a = new PaymentRequest.Counterparty("A", "Alice");
        var b = new PaymentRequest.Counterparty("A", "Alice");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals("Counterparty[accountId=A, name=Alice]", a.toString());
    }
}
