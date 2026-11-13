package module01.exercise2;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CardFeeContextTest {

    @Test
    void percentageFeeForStandardCustomer() {
        CardFeeContext ctx = new CardFeeContext(new PercentageFee(new BigDecimal("0.02")));
        assertEquals(new BigDecimal("2.00"), ctx.calculateFee(new BigDecimal("100"), CustomerTier.STANDARD));
    }

    @Test
    void premiumCustomerGetsFiftyPercentDiscount() {
        CardFeeContext ctx = new CardFeeContext(new PercentageFee(new BigDecimal("0.02")));
        assertEquals(new BigDecimal("1.00"), ctx.calculateFee(new BigDecimal("100"), CustomerTier.PREMIUM));
        assertEquals(new BigDecimal("2.00"), ctx.calculateFee(new BigDecimal("100"), CustomerTier.PLUS));
    }

    @Test
    void strategyCanBeSwappedAtRuntime() {
        CardFeeContext ctx = new CardFeeContext(new FlatFee(new BigDecimal("0.30")));
        assertEquals(new BigDecimal("0.30"), ctx.calculateFee(new BigDecimal("5000"), CustomerTier.STANDARD));
        ctx.setStrategy(new TieredFee(new BigDecimal("0.01"), new BigDecimal("0.50"), new BigDecimal("10.00")));
        assertEquals(new BigDecimal("0.50"), ctx.calculateFee(new BigDecimal("10"), CustomerTier.STANDARD));   // floor
        assertEquals(new BigDecimal("10.00"), ctx.calculateFee(new BigDecimal("5000"), CustomerTier.STANDARD)); // cap
        assertEquals(new BigDecimal("5.00"), ctx.calculateFee(new BigDecimal("500"), CustomerTier.STANDARD));
        assertEquals(new BigDecimal("5.00"), ctx.calculateFee(new BigDecimal("5000"), CustomerTier.PREMIUM));
    }

    @Test
    void halfPenniesRoundHalfUp() {
        CardFeeContext ctx = new CardFeeContext(new FlatFee(new BigDecimal("0.01")));
        assertEquals(new BigDecimal("0.01"), ctx.calculateFee(BigDecimal.TEN, CustomerTier.PREMIUM)); // 0.005 -> 0.01
    }

    @Test
    void rejectsInvalidInput() {
        CardFeeContext ctx = new CardFeeContext(new FlatFee(BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> ctx.calculateFee(new BigDecimal("-1"), CustomerTier.STANDARD));
        assertThrows(NullPointerException.class, () -> ctx.calculateFee(BigDecimal.ONE, null));
        assertThrows(NullPointerException.class, () -> new CardFeeContext(null));
        assertThrows(IllegalArgumentException.class, () -> new PercentageFee(new BigDecimal("-0.1")));
        assertThrows(IllegalArgumentException.class, () -> new TieredFee(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE));
    }
}
