package module02.immutability;

import module02.immutability.bad.MutableFxQuote;
import module02.immutability.good.FxQuote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImmutabilityTest {

    @Test
    void badQuoteLeaksInternals() {
        List<String> venues = new ArrayList<>(List.of("LMAX"));
        Date until = new Date(1_000L);
        MutableFxQuote q = new MutableFxQuote("GBPUSD", BigDecimal.ONE, until, venues);
        q.getVenues().add("EBS");
        q.getValidUntil().setTime(0L);
        assertEquals(2, q.getVenues().size());
        assertEquals(0L, q.getValidUntil().getTime());
    }

    @Test
    void goodQuoteIsDeeplyImmutable() {
        List<String> venues = new ArrayList<>(List.of("LMAX"));
        FxQuote q = new FxQuote("GBPUSD", new BigDecimal("1.25"), Instant.ofEpochSecond(100), venues);
        venues.add("EBS");
        assertEquals(List.of("LMAX"), q.venues());
        assertThrows(UnsupportedOperationException.class, () -> q.venues().add("X"));
        FxQuote q2 = q.withRate(new BigDecimal("1.26"));
        assertEquals(new BigDecimal("1.25"), q.rate());
        assertEquals(new BigDecimal("1.26"), q2.rate());
        assertTrue(q.isValidAt(Instant.ofEpochSecond(100)));
        assertFalse(q.isValidAt(Instant.ofEpochSecond(101)));
        assertThrows(IllegalArgumentException.class, () -> q.withRate(BigDecimal.ZERO));
    }

    @Test
    void goodQuoteEqualityIgnoresScale() {
        FxQuote a = new FxQuote("GBPUSD", new BigDecimal("1.250"), Instant.EPOCH, List.of());
        FxQuote b = new FxQuote("GBPUSD", new BigDecimal("1.25"), Instant.EPOCH, List.of());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
