package module14.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class MinorUnitFeesTest {

    @Test
    void halfEvenEdgeCases() {
        // 1.45% of 1.00 EUR = 1.45 cents -> 1 ; of 10.00 = 14.5 cents -> 14 (even) ; of 30.00 = 43.5 -> 44 (even)
        assertEquals(1, MinorUnitFees.feeMinorUnits(100, 145));
        assertEquals(14, MinorUnitFees.feeMinorUnits(1_000, 145));
        assertEquals(44, MinorUnitFees.feeMinorUnits(3_000, 145));
        assertEquals(-14, MinorUnitFees.feeMinorUnits(-1_000, 145), "refunds round symmetrically");
        assertEquals(0, MinorUnitFees.feeMinorUnits(0, 145));
    }

    @Test
    void propertyMinorUnitsMatchBigDecimalReference() {
        SplittableRandom rnd = new SplittableRandom(2024);
        for (int i = 0; i < 200_000; i++) {
            long cents = rnd.nextLong(-10_000_000_000L, 10_000_000_000L);
            int bps = rnd.nextInt(0, 2_000);
            BigDecimal reference = MinorUnitFees.feeBigDecimal(BigDecimal.valueOf(cents, 2), BigDecimal.valueOf(bps, 2));
            assertEquals(reference.movePointRight(2).longValueExact(), MinorUnitFees.feeMinorUnits(cents, bps),
                    () -> cents + " cents @ " + bps + " bps");
        }
    }

    @Test
    void overflowIsDetectedNotWrapped() {
        assertThrows(ArithmeticException.class, () -> MinorUnitFees.feeMinorUnits(Long.MAX_VALUE / 10, 10_000));
        assertThrows(ArithmeticException.class, () -> MinorUnitFees.sumFeesMinor(new long[]{Long.MAX_VALUE / 2, Long.MAX_VALUE / 2}, 10_000));
    }

    @Test
    void batchSumsAgree() {
        long[] minor = {1_000, 2_550, 99_999, 12};
        BigDecimal[] big = {new BigDecimal("10.00"), new BigDecimal("25.50"), new BigDecimal("999.99"), new BigDecimal("0.12")};
        assertEquals(MinorUnitFees.sumFeesBigDecimal(big, new BigDecimal("1.45")).movePointRight(2).longValueExact(),
                MinorUnitFees.sumFeesMinor(minor, 145));
    }
}
