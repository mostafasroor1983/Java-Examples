package module18.ch09;

import java.math.BigDecimal;

/**
 * Item 60 - Avoid float and double if exact answers are required.
 *
 * <p>Binary floating point cannot represent 0.10 exactly: {@code 1.03 - 0.42} is {@code 0.6100000000000001}. For money
 * use {@link BigDecimal} (constructed from Strings), or {@code long}/{@code int} minor units when performance matters
 * and the range fits. double is fine for statistics, risk models and ML features, never for balances.
 */
public final class Item60AvoidFloatDouble {
    private Item60AvoidFloatDouble() {
    }

    /** How many 0.10 items can you buy with 1.00? (classic) */
    public static int itemsBoughtWithDouble() {
        double funds = 1.00;
        int items = 0;
        for (double price = 0.10; funds >= price; price += 0.10) {
            funds -= price;
            items++;
        }
        return items; // 3, with 0.3999999999999999 left: wrong
    }

    public static int itemsBoughtWithBigDecimal() {
        BigDecimal funds = new BigDecimal("1.00");
        int items = 0;
        for (BigDecimal price = new BigDecimal("0.10"); funds.compareTo(price) >= 0; price = price.add(new BigDecimal("0.10"))) {
            funds = funds.subtract(price);
            items++;
        }
        return items; // 4, nothing left
    }

    public static int itemsBoughtWithCents() {
        int funds = 100, items = 0;
        for (int price = 10; funds >= price; price += 10) {
            funds -= price;
            items++;
        }
        return items;
    }
}
