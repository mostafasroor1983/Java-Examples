package module18.ch06;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Item 38 - Emulate extensible enums with interfaces.
 *
 * <p>Enums cannot be extended, but an enum can implement an interface. The core library ships {@code BasicFeeRule};
 * a partner team adds {@code PromoFeeRule} in its own enum, and every API that accepts {@code FeeRule} (or
 * {@code Collection<? extends FeeRule>}) works with both. The bounded type token
 * {@code <T extends Enum<T> & FeeRule>} lets a method iterate all constants of any such enum.
 */
public final class Item38ExtensibleEnums {
    private Item38ExtensibleEnums() {
    }

    public interface FeeRule {
        BigDecimal apply(BigDecimal amount);
    }

    public enum BasicFeeRule implements FeeRule {
        NONE {
            public BigDecimal apply(BigDecimal amount) { return BigDecimal.ZERO.setScale(2); }
        },
        ONE_PERCENT {
            public BigDecimal apply(BigDecimal amount) { return amount.movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN); }
        }
    }

    public enum PromoFeeRule implements FeeRule {
        FIRST_MONTH_FREE {
            public BigDecimal apply(BigDecimal amount) { return BigDecimal.ZERO.setScale(2); }
        },
        CAPPED_TWO_EUR {
            public BigDecimal apply(BigDecimal amount) {
                return amount.movePointLeft(2).min(new BigDecimal("2")).setScale(2, RoundingMode.HALF_EVEN);
            }
        }
    }

    public static <T extends Enum<T> & FeeRule> BigDecimal cheapest(Class<T> ruleSet, BigDecimal amount) {
        return cheapest(java.util.List.of(ruleSet.getEnumConstants()), amount);
    }

    public static BigDecimal cheapest(Collection<? extends FeeRule> rules, BigDecimal amount) {
        return rules.stream().map(r -> r.apply(amount)).min(BigDecimal::compareTo).orElseThrow();
    }
}
