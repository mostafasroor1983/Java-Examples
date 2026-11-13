package module06.exercise3;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FxConversionResultTest {
    private final FxConversionResult fx = new FxConversionResult("GBP", Map.of(
            "USD", new BigDecimal("0.80"),
            "EUR", new BigDecimal("0.85")));

    @Test
    void convertsKnownCurrenciesAndCollectsFailures() {
        var out = fx.convertAll(List.of(
                new FxConversionResult.Balance("a1", "USD", new BigDecimal("100")),
                new FxConversionResult.Balance("a2", "EUR", new BigDecimal("200")),
                new FxConversionResult.Balance("a3", "GBP", new BigDecimal("50")),
                new FxConversionResult.Balance("a4", "JPY", new BigDecimal("1000"))));
        assertEquals(3, out.converted().size());
        assertEquals(1, out.failed().size());
        assertEquals("a4", out.failed().get(0).accountId());
        assertEquals(new BigDecimal("300.00"), out.total()); // 80 + 170 + 50
    }

    @Test
    void totalsByCurrency() {
        var totals = fx.totalsByOriginalCurrency(List.of(
                new FxConversionResult.Balance("a1", "USD", new BigDecimal("100")),
                new FxConversionResult.Balance("a2", "USD", new BigDecimal("25")),
                new FxConversionResult.Balance("a3", "EUR", new BigDecimal("100"))));
        assertEquals(new BigDecimal("100.00"), totals.get("USD"));
        assertEquals(new BigDecimal("85.00"), totals.get("EUR"));
        assertEquals(List.of("EUR", "USD"), List.copyOf(totals.keySet()));
    }

    @Test
    void emptyBatch() {
        var out = fx.convertAll(List.of());
        assertEquals(BigDecimal.ZERO, out.total());
        assertTrue(out.failed().isEmpty());
    }
}
