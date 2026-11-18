package module18.ch09;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Item 68 - Adhere to generally accepted naming conventions.
 *
 * <p>Typographical: packages lower case, classes/interfaces UpperCamel, methods/fields lowerCamel, constants
 * UPPER_SNAKE, type parameters single capitals (T, E, K, V, R, X). Grammatical: classes are nouns; methods are verbs;
 * boolean queries start with {@code is}/{@code has}; conversions {@code toX} (new object), views {@code asX},
 * primitive extraction {@code xValue}; static factories {@code of}, {@code from}, {@code valueOf}, {@code newInstance},
 * {@code getInstance}.
 */
public final class Item68NamingConventions {
    public static final int MAX_DAILY_TRANSFERS = 50;

    private Item68NamingConventions() {
    }

    public record AccountNumber(String value) {
        public static AccountNumber of(String value) {
            return new AccountNumber(value);
        }

        public static AccountNumber from(String iban) {
            return new AccountNumber(iban.substring(iban.length() - 10));
        }

        public boolean isDomestic() {
            return value.startsWith("00");
        }

        public String toMaskedString() {
            return "******" + value.substring(Math.max(0, value.length() - 4));
        }
    }

    public static <K, V> Optional<V> lookup(java.util.Map<K, V> map, K key) {
        return Optional.ofNullable(map.get(key));
    }

    public static long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}
