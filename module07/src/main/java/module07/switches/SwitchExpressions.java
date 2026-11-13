package module07.switches;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/** Arrow-form switch expressions: no fall-through, must be exhaustive, yield for blocks. */
public final class SwitchExpressions {
    private SwitchExpressions() {
    }

    public enum Tier { STANDARD, PLUS, PREMIUM, METAL }

    /** Java 8: statement switch with break; forgetting break is a silent bug. */
    public static int freeAtmWithdrawalsOld(Tier tier) {
        int n;
        switch (tier) {
            case STANDARD:
                n = 1;
                break;
            case PLUS:
                n = 3;
                break;
            case PREMIUM:
            case METAL:
                n = 10;
                break;
            default:
                throw new IllegalArgumentException();
        }
        return n;
    }

    /** Java 14+: expression, exhaustive over enum, no default needed. */
    public static int freeAtmWithdrawals(Tier tier) {
        return switch (tier) {
            case STANDARD -> 1;
            case PLUS -> 3;
            case PREMIUM, METAL -> 10;
        };
    }

    public static BigDecimal fxMarkupPercent(Tier tier, DayOfWeek day) {
        boolean weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        return switch (tier) {
            case STANDARD -> {
                BigDecimal base = new BigDecimal("1.0");
                yield weekend ? base.add(new BigDecimal("1.0")) : base;
            }
            case PLUS -> weekend ? new BigDecimal("1.0") : new BigDecimal("0.5");
            case PREMIUM, METAL -> BigDecimal.ZERO.setScale(1);
        };
    }

    /** Switch on strings still works; `default` is mandatory for non-sealed, non-enum selectors. */
    public static String mccCategory(String mcc) {
        return switch (mcc) {
            case "5411", "5499" -> "groceries";
            case "4111", "4121" -> "transport";
            case "6011" -> "atm";
            default -> "other";
        };
    }
}
