package module01.abstraction;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProcessorTest {

    @Test
    void cardProcessorProducesCardReference() {
        PaymentProcessor p = new CardPaymentProcessor();
        PaymentReceipt r = p.process("p1", new BigDecimal("10.00"), "GBP");
        assertEquals("CARD", r.rail());
        assertEquals("CARD-1", r.reference());
    }

    @Test
    void bankProcessorProducesSepaReference() {
        PaymentProcessor p = new BankTransferProcessor();
        assertEquals("SEPA-1", p.process("p1", BigDecimal.ONE, "EUR").reference());
        assertEquals("SEPA-2", p.process("p2", BigDecimal.ONE, "EUR").reference());
    }

    @Test
    void validationIsEnforcedByTemplate() {
        PaymentProcessor p = new CardPaymentProcessor();
        assertThrows(IllegalArgumentException.class, () -> p.process("", BigDecimal.ONE, "GBP"));
        assertThrows(IllegalArgumentException.class, () -> p.process("p", BigDecimal.ZERO, "GBP"));
        assertThrows(IllegalArgumentException.class, () -> p.process("p", BigDecimal.ONE, "GB"));
    }
}
