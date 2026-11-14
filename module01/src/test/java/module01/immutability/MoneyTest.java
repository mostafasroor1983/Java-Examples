package module01.immutability;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void scaleIsNormalised() {
        assertEquals(Money.of("10.0", "GBP"), Money.of("10.00", "GBP"));
        assertEquals(Money.of("10.0", "GBP").hashCode(), Money.of("10.00", "GBP").hashCode());
    }

    @Test
    void operationsReturnNewInstances() {
        Money a = Money.of("10", "GBP");
        Money b = a.plus(Money.of("5", "GBP"));
        assertEquals(Money.of("10", "GBP"), a);
        assertEquals(Money.of("15", "GBP"), b);
    }

    @Test
    void currencyMismatchIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of("1", "GBP").plus(Money.of("1", "EUR")));
    }

    @Test
    void jpyHasZeroScale() {
        assertEquals(0, Money.of("100", "JPY").amount().scale());
    }

    @Test
    void transferValidatesInvariants() {
        Money amt = Money.of("10", "GBP");
        assertThrows(IllegalArgumentException.class,
                () -> new Transfer("t", "A", "A", amt, Instant.EPOCH));
        assertThrows(IllegalArgumentException.class,
                () -> new Transfer("t", "A", "B", Money.zero(Currency.getInstance("GBP")), Instant.EPOCH));
        Transfer t = new Transfer("t", "A", "B", amt, Instant.EPOCH);
        Transfer t2 = t.withAmount(Money.of("20", "GBP"));
        assertEquals(new BigDecimal("10.00"), t.amount().amount());
        assertEquals(new BigDecimal("20.00"), t2.amount().amount());
    }
}
