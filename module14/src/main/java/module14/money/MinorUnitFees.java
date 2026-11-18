package module14.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fee = amount x rate, rounded HALF_EVEN to the minor unit, computed two ways.
 *
 * <ul>
 *   <li>{@link #feeBigDecimal}: clear, arbitrary precision, allocates several objects per call.</li>
 *   <li>{@link #feeMinorUnits}: amount in cents ({@code long}) and rate in basis points ({@code int}, 1 bp = 0.01%).
 *   No allocation, and roughly an order of magnitude faster in hot loops (card authorisation, interest accrual over millions of
 *   accounts). The price is that you must handle overflow ({@link Math#multiplyExact}) and rounding yourself.</li>
 * </ul>
 * Rule of thumb: BigDecimal everywhere by default. Minor units only on measured hot paths, behind a well-tested API,
 * and verified against the BigDecimal reference (see the property test).
 */
public final class MinorUnitFees {
    private MinorUnitFees() {
    }

    public static BigDecimal feeBigDecimal(BigDecimal amount, BigDecimal ratePercent) {
        return amount.multiply(ratePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
    }

    /**
     * @param amountMinor amount in minor units (cents), may be negative (refund fees)
     * @param rateBps     rate in basis points (e.g. 145 = 1.45%)
     * @return fee in minor units, HALF_EVEN rounded
     * @throws ArithmeticException on overflow instead of silently wrapping around
     */
    public static long feeMinorUnits(long amountMinor, int rateBps) {
        long scaled = Math.multiplyExact(amountMinor, (long) rateBps); // cents x bps = fee in 1/10_000 of a cent
        long q = scaled / 10_000;
        long r = scaled % 10_000;                                     // same sign as scaled
        long absR2 = Math.abs(r) * 2;
        if (absR2 > 10_000 || (absR2 == 10_000 && (q & 1) != 0)) {   // > half, or exactly half and q is odd
            q += Long.signum(scaled);
        }
        return q;
    }

    public static long sumFeesMinor(long[] amountsMinor, int rateBps) {
        long total = 0;
        for (long a : amountsMinor) total = Math.addExact(total, feeMinorUnits(a, rateBps));
        return total;
    }

    public static BigDecimal sumFeesBigDecimal(BigDecimal[] amounts, BigDecimal ratePercent) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal a : amounts) total = total.add(feeBigDecimal(a, ratePercent));
        return total;
    }
}
