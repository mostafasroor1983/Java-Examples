package module01.polymorphism;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FeeCalculatorTest {

    @Test
    void dispatchPicksSubtypeBehaviour() {
        assertEquals(new BigDecimal("1.50"), FeeCalculator.feeViaDispatch(new Card("****1111", "VISA"), new BigDecimal("100")));
        assertEquals(new BigDecimal("0.20"), FeeCalculator.feeViaDispatch(new BankAccount("GB00"), new BigDecimal("100")));
        assertEquals(new BigDecimal("0.00"), FeeCalculator.feeViaDispatch(new Wallet("w1"), new BigDecimal("100")));
    }

    @Test
    void patternMatchingIsExhaustive() {
        assertEquals("HIGH", FeeCalculator.riskBand(new Card("x", "VISA"), new BigDecimal("1000.01")));
        assertEquals("MEDIUM", FeeCalculator.riskBand(new Card("x", "VISA"), new BigDecimal("1000")));
        assertEquals("LOW", FeeCalculator.riskBand(new BankAccount("x"), new BigDecimal("1000000")));
        assertEquals("LOW", FeeCalculator.riskBand(new Wallet("x"), BigDecimal.ONE));
    }
}
