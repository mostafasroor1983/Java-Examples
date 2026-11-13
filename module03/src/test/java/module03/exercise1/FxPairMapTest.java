package module03.exercise1;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FxPairMapTest {
    private FxPairMap map() {
        FxPairMap m = new FxPairMap();
        m.put("GBP", "USD", new BigDecimal("1.25"));
        m.put("USD", "EUR", new BigDecimal("0.90"));
        return m;
    }

    @Test
    void directInverseAndCross() {
        FxPairMap m = map();
        assertEquals(new BigDecimal("1.25"), m.rate("GBP", "USD").orElseThrow());
        assertEquals(new BigDecimal("0.80000000"), m.rate("USD", "GBP").orElseThrow());
        assertEquals(new BigDecimal("1.12500000"), m.rate("GBP", "EUR").orElseThrow()); // 1.25 * 0.90
        assertEquals(BigDecimal.ONE, m.rate("GBP", "GBP").orElseThrow());
        assertTrue(m.rate("GBP", "JPY").isEmpty());
    }

    @Test
    void pairValidation() {
        assertThrows(IllegalArgumentException.class, () -> new FxPairMap.Pair("GBP", "GBP"));
        assertThrows(IllegalArgumentException.class, () -> new FxPairMap.Pair("GB", "USD"));
        assertThrows(IllegalArgumentException.class, () -> map().put("GBP", "JPY", BigDecimal.ZERO));
        assertEquals(new FxPairMap.Pair("USD", "GBP"), new FxPairMap.Pair("GBP", "USD").inverse());
    }
}
