package module07.patterns;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatternMatchingTest {
    private final PatternMatching.Card visa = new PatternMatching.Card("4111", "VISA");
    private final PatternMatching.Card amex = new PatternMatching.Card("3700", "AMEX");
    private final PatternMatching.Iban iban = new PatternMatching.Iban("GB00", "GB");
    private final PatternMatching.Crypto btc = new PatternMatching.Crypto("bc1q", "BTC");

    @Test
    void oldAndNewInstanceofAgree() {
        assertEquals("card-VISA", PatternMatching.routeOld(visa));
        assertEquals("card-VISA", PatternMatching.routeInstanceof(visa));
        assertEquals("card-amex-special", PatternMatching.routeInstanceof(amex));
        assertEquals("sepa-GB", PatternMatching.routeInstanceof(iban));
        assertEquals("unknown", PatternMatching.routeInstanceof("x"));
    }

    @Test
    void nestedRecordPatternsWithGuards() {
        assertEquals("large card payout via VISA", PatternMatching.describe(new PatternMatching.Payout(visa, new BigDecimal("20000"))));
        assertEquals("card payout via AMEX", PatternMatching.describe(new PatternMatching.Payout(amex, BigDecimal.ONE)));
        assertEquals("bank payout to GB", PatternMatching.describe(new PatternMatching.Payout(iban, BigDecimal.ONE)));
        assertEquals("crypto payout on BTC", PatternMatching.describe(new PatternMatching.Payout(btc, BigDecimal.ONE)));
    }

    @Test
    void nullCaseAndAccumulation() {
        assertEquals("no instrument", PatternMatching.nullSafe(null));
        assertEquals("card", PatternMatching.nullSafe(visa));
        var payouts = List.of(new PatternMatching.Payout(visa, BigDecimal.ONE), new PatternMatching.Payout(iban, BigDecimal.TEN),
                new PatternMatching.Payout(amex, new BigDecimal("2")));
        assertEquals(new BigDecimal("3"), PatternMatching.totalCardPayouts(payouts));
    }
}
