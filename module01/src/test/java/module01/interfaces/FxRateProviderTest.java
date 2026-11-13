package module01.interfaces;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FxRateProviderTest {
    private final FxRateProvider primary = new InMemoryFxRateProvider(Map.of("GBP/USD", new BigDecimal("1.25")));

    @Test
    void defaultMethodsDeriveBehaviour() {
        assertEquals(new BigDecimal("1.25"), primary.rateOrThrow("GBP", "USD"));
        assertEquals(new BigDecimal("0.8000000000"), primary.inverseRate("USD", "GBP").orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> primary.rateOrThrow("GBP", "JPY"));
    }

    @Test
    void fallbackChaining() {
        FxRateProvider chained = primary.orElse(FxRateProvider.fixed("GBP", "EUR", new BigDecimal("1.15")));
        assertEquals(new BigDecimal("1.25"), chained.rateOrThrow("GBP", "USD"));
        assertEquals(new BigDecimal("1.15"), chained.rateOrThrow("GBP", "EUR"));
        assertTrue(chained.rate("GBP", "JPY").isEmpty());
    }
}
