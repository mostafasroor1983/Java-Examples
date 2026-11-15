package module02.valueobjects;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValueObjectsTest {

    @Test
    void bigDecimalEqualsIsScaleSensitiveButMoneyIsNot() {
        assertNotEquals(new BigDecimal("1.0"), new BigDecimal("1.00"));
        assertEquals(0, new BigDecimal("1.0").compareTo(new BigDecimal("1.00")));
        assertEquals(Money.of("1.0", "GBP"), Money.of("1.00", "GBP"));
        assertEquals(Money.of("1", "GBP").hashCode(), Money.of("1.00", "GBP").hashCode());
    }

    @Test
    void moneyRejectsPrecisionLoss() {
        assertThrows(ArithmeticException.class, () -> Money.of("1.005", "GBP"));
    }

    @Test
    void moneyArithmeticAndCurrencyGuard() {
        assertEquals(Money.of("15.00", "GBP"),
                Money.of("10", "GBP").add(Money.of("5", "GBP")));
        assertThrows(IllegalArgumentException.class,
                () -> Money.of("1", "GBP").add(Money.of("1", "USD")));
        assertTrue(Money.of("1", "GBP").compareTo(Money.of("0.99", "GBP")) > 0);
        assertFalse(Money.of("0", "GBP").isPositive());
        assertTrue(Money.of("0.01", "GBP").isPositive());
    }

    @Test
    void emailIsNormalised() {
        assertEquals(Email.of("  Bob@Example.COM "), Email.of("bob@example.com"));
        assertThrows(IllegalArgumentException.class, () -> Email.of("not-an-email"));
        assertThrows(IllegalArgumentException.class, () -> Email.of("a@b"));
        assertThrows(NullPointerException.class, () -> Email.of(null));
    }

    @Test
    void customerIdIsTypedAndHashable() {
        CustomerId id = CustomerId.random();
        CustomerId same = CustomerId.parse(id.value().toString());
        assertEquals(id, same);
        Set<CustomerId> set = new HashSet<>(Set.of(id));
        assertTrue(set.contains(same));
        assertThrows(IllegalArgumentException.class, () -> CustomerId.parse("nope"));
    }
}
