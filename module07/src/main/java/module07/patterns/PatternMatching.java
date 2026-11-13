package module07.patterns;

import java.math.BigDecimal;
import java.util.List;

/** instanceof patterns, record patterns (nested), guarded patterns, null handling in switch. */
public final class PatternMatching {
    private PatternMatching() {
    }

    public sealed interface Instrument permits Card, Iban, Crypto {
    }

    public record Card(String pan, String scheme) implements Instrument {
    }

    public record Iban(String value, String country) implements Instrument {
    }

    public record Crypto(String address, String network) implements Instrument {
    }

    public record Payout(Instrument to, BigDecimal amount) {
    }

    /** Java 8 style. */
    public static String routeOld(Object o) {
        if (o instanceof Card) {
            Card c = (Card) o;
            return "card-" + c.scheme();
        } else if (o instanceof Iban) {
            Iban i = (Iban) o;
            return "sepa-" + i.country();
        }
        return "unknown";
    }

    /** instanceof pattern with binding + flow scoping. */
    public static String routeInstanceof(Object o) {
        if (o instanceof Card c && c.scheme().equals("AMEX")) return "card-amex-special";
        if (o instanceof Card c) return "card-" + c.scheme();
        if (!(o instanceof Iban i)) return "unknown";
        return "sepa-" + i.country(); // i is in scope here because the negated branch returned
    }

    /** Nested record patterns deconstruct two levels at once. */
    public static String describe(Payout p) {
        return switch (p) {
            case Payout(Card(var pan, var scheme), var amt) when amt.compareTo(new BigDecimal("10000")) > 0 -> "large card payout via " + scheme;
            case Payout(Card(var pan, var scheme), var amt) -> "card payout via " + scheme;
            case Payout(Iban(var v, var country), var amt) -> "bank payout to " + country;
            case Payout(Crypto(var addr, var net), var amt) -> "crypto payout on " + net;
        };
    }

    /** Switch can match null explicitly instead of throwing NPE. */
    public static String nullSafe(Instrument i) {
        return switch (i) {
            case null -> "no instrument";
            case Card c -> "card";
            case Iban x -> "iban";
            case Crypto x -> "crypto";
        };
    }

    public static BigDecimal totalCardPayouts(List<Payout> payouts) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Payout p : payouts) {
            if (p instanceof Payout(Card c, BigDecimal amt)) sum = sum.add(amt);
        }
        return sum;
    }
}
