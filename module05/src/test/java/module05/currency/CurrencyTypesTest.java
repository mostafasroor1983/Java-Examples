package module05.currency;

import module05.currency.CurrencyUnit.EUR;
import module05.currency.CurrencyUnit.GBP;
import module05.currency.CurrencyUnit.JPY;
import module05.currency.CurrencyUnit.USD;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyTypesTest {

    @Test
    void sameCurrencyArithmetic() {
        Amount<EUR> a = Amount.of("10.10", EUR.INSTANCE);
        Amount<EUR> b = Amount.of("0.9", EUR.INSTANCE);
        assertEquals(Amount.of("11.00", EUR.INSTANCE), a.plus(b));
        assertEquals("9.20 EUR", a.minus(b).toString());
        // a.plus(Amount.of("1", USD.INSTANCE));  <- does not compile: that is the whole point
    }

    @Test
    void conversionRoundsOnceToTargetScale() {
        FxRate<EUR, USD> eurUsd = new FxRate<>(EUR.INSTANCE, USD.INSTANCE, new BigDecimal("1.08505"));
        Amount<USD> usd = eurUsd.convert(Amount.of("100.00", EUR.INSTANCE));
        assertEquals(Amount.of("108.50", USD.INSTANCE), usd, "HALF_EVEN: 108.505 -> 108.50");
        FxRate<USD, JPY> usdJpy = new FxRate<>(USD.INSTANCE, JPY.INSTANCE, new BigDecimal("150.123"));
        Amount<JPY> jpy = eurUsd.then(usdJpy).convert(Amount.of("1.00", EUR.INSTANCE));
        assertEquals(Amount.of("163", JPY.INSTANCE), jpy);
    }

    @Test
    void inverseRoundTripsWithinOneMinorUnit() {
        FxRate<GBP, USD> r = new FxRate<>(GBP.INSTANCE, USD.INSTANCE, new BigDecimal("1.2671"));
        Amount<GBP> start = Amount.of("12345.67", GBP.INSTANCE);
        Amount<GBP> back = r.inverse().convert(r.convert(start));
        assertTrue(back.value().subtract(start.value()).abs().compareTo(new BigDecimal("0.01")) <= 0);
    }

    @Test
    void untypedAmountsAreCheckedAtRuntime() {
        Amount<?> fromDb = Amount.of("5.00", USD.INSTANCE);
        assertEquals(Amount.of("5.00", USD.INSTANCE), Amount.as(fromDb, USD.INSTANCE));
        assertThrows(IllegalArgumentException.class, () -> Amount.as(fromDb, EUR.INSTANCE));
    }

    @Test
    void invalidValues() {
        assertThrows(ArithmeticException.class, () -> Amount.of("1.005", EUR.INSTANCE));
        assertThrows(ArithmeticException.class, () -> Amount.of("1.5", JPY.INSTANCE));
        assertThrows(IllegalArgumentException.class, () -> new FxRate<>(EUR.INSTANCE, USD.INSTANCE, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new FxRate<>(EUR.INSTANCE, EUR.INSTANCE, BigDecimal.ONE));
    }
}
