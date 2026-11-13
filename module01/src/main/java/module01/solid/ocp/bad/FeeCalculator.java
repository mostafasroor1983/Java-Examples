package module01.solid.ocp.bad;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BAD: every new payment type means editing this switch - the class is never "closed".
 * Default branch silently hides a missing case.
 */
public class FeeCalculator {
    public BigDecimal fee(String type, BigDecimal amount) {
        switch (type) {
            case "CARD":
                return amount.multiply(new BigDecimal("0.015")).setScale(2, RoundingMode.HALF_UP);
            case "BANK":
                return new BigDecimal("0.20");
            case "CRYPTO":
                return amount.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
            default:
                return BigDecimal.ZERO; // a typo in `type` silently yields zero fee
        }
    }
}
