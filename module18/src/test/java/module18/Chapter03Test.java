package module18;

import module18.ch03.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Chapter 3 - Methods common to all objects (items 10-14). */
class Chapter03Test {

    @Test
    void item10_symmetryBrokenVsFixed() {
        var broken = new Item10EqualsContract.BrokenCaseInsensitiveRef("Inv-1");
        assertTrue(broken.equals("inv-1"));
        assertFalse("inv-1".equals(broken), "not symmetric");
        assertEquals(new Item10EqualsContract.PaymentReference(" inv-1"), new Item10EqualsContract.PaymentReference("INV-1"));
        assertNotEquals(null, new Item10EqualsContract.PaymentReference("x"));
    }

    @Test
    void item11_hashSetFindsEqualKeyOnlyWithHashCode() {
        Set<Object> broken = new HashSet<>(List.of(new Item11HashCode.BrokenAccountKey("B1", "123")));
        // Identity hashes almost surely differ, so the equal key lands in another bucket.
        assertFalse(broken.contains(new Item11HashCode.BrokenAccountKey("B1", "123")));
        Set<Object> good = new HashSet<>(List.of(new Item11HashCode.AccountKey("B1", "123")));
        assertTrue(good.contains(new Item11HashCode.AccountKey("B1", "123")));
    }

    @Test
    void item12_informativeAndSafeToString() {
        var t = new Item12ToString.Transfer("T-1", "DE89370400440532013000", "FR7630006000011234567890189", new BigDecimal("1E+2"), "EUR");
        assertEquals("Transfer[ref=T-1, from=****3000, to=****0189, amount=100 EUR]", t.toString());
    }

    @Test
    void item13_deepCopyVsShallow() {
        var original = new Item13Clone.ExposureLimits(1_000, 5_000);
        var clone = original.clone();
        var copy = Item13Clone.ExposureLimits.copyOf(original);
        original.setLimit(0, 1);
        assertEquals(1_000, clone.limit(0));
        assertEquals(1_000, copy.limit(0));
    }

    @Test
    void item14_naturalOrderAndOverflow() {
        var d = LocalDate.of(2024, 3, 1);
        var list = new ArrayList<>(List.of(
                new Item14Comparable.Transaction("b", d.plusDays(1), BigDecimal.ONE),
                new Item14Comparable.Transaction("a", d, BigDecimal.ONE),
                new Item14Comparable.Transaction("c", d, BigDecimal.TEN)));
        Collections.sort(list);
        assertEquals(List.of("c", "a", "b"), list.stream().map(Item14Comparable.Transaction::id).toList());
        // 3,000,000,000 cents (30M EUR) vs 0: the long difference does not fit in an int -> negative after the cast
        assertTrue(Item14Comparable.BROKEN_BY_SUBTRACTION.compare(3_000_000_000L, 0L) < 0, "overflow: wrong sign");
        assertEquals(0, Item14Comparable.BROKEN_BY_SUBTRACTION.compare(1L << 32, 0L), "or claims different values are equal");
        assertTrue(Item14Comparable.SAFE.compare(3_000_000_000L, 0L) > 0);
    }
}
