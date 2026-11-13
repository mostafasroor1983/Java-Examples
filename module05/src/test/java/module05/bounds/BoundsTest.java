package module05.bounds;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoundsTest {

    @Test
    void multipleBounds() {
        List<Fee> fees = List.of(new Fee(new BigDecimal("1")), new Fee(new BigDecimal("2")), new Fee(new BigDecimal("3")));
        assertEquals(new Fee(new BigDecimal("6")), Bounds.sumIfAscending(fees));
        assertThrows(IllegalArgumentException.class,
                () -> Bounds.sumIfAscending(List.of(new Fee(BigDecimal.TEN), new Fee(BigDecimal.ONE))));
        assertThrows(IllegalArgumentException.class, () -> Bounds.sumIfAscending(List.<Fee>of()));
    }

    @Test
    void isSortedWorksForComparableTypes() {
        assertTrue(Bounds.isSorted(List.of(1, 2, 2, 3)));
        assertFalse(Bounds.isSorted(List.of("b", "a")));
        assertTrue(Bounds.isSorted(List.of(Money.of("1", "GBP"), Money.of("2", "GBP"))));
        assertTrue(Bounds.isSorted(List.of()));
    }

    @Test
    void recursiveBoundKeepsFluentSubtype() {
        String built = new Bounds.CardPaymentBuilder()
                .id("p1")              // returns CardPaymentBuilder, not PaymentBuilder
                .amount(Money.of("10", "GBP"))
                .pan("****1111")
                .build();
        assertEquals("CARD p1 10 GBP ****1111", built);
    }
}
