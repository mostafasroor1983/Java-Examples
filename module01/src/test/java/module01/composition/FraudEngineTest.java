package module01.composition;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FraudEngineTest {
    private final FraudEngine engine = new FraudEngine(List.of(
            StandardChecks.amountAbove(new BigDecimal("1000")),
            StandardChecks.blockedCountries(Set.of("KP")),
            StandardChecks.blockedCustomers(Set.of("c-bad"))));

    @Test
    void approvesCleanPayment() {
        var decision = engine.evaluate("c-1", new BigDecimal("50"), "GB");
        assertTrue(decision.approved());
        assertTrue(decision.reasons().isEmpty());
    }

    @Test
    void collectsAllReasons() {
        var decision = engine.evaluate("c-bad", new BigDecimal("5000"), "KP");
        assertFalse(decision.approved());
        assertEquals(3, decision.reasons().size());
    }

    @Test
    void engineWithNoChecksApprovesEverything() {
        assertTrue(new FraudEngine(List.of()).evaluate("x", BigDecimal.TEN, "XX").approved());
    }
}
