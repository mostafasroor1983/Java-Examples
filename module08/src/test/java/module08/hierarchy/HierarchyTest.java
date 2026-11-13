package module08.hierarchy;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class HierarchyTest {

    @Test
    void checkedExceptionsPropagateAndWrap() throws IOException {
        assertEquals("statement:a.csv", CheckedVsUnchecked.loadStatement("a.csv"));
        assertThrows(FileNotFoundException.class, () -> CheckedVsUnchecked.loadStatement("x.missing"));
        UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> CheckedVsUnchecked.loadStatementUnchecked(" "));
        assertInstanceOf(IOException.class, e.getCause());
    }

    @Test
    void uncheckedForValidationErrorForInvariants() {
        assertEquals(BigDecimal.ONE, CheckedVsUnchecked.requirePositive(BigDecimal.ONE));
        InvalidPaymentException e = assertThrows(InvalidPaymentException.class, () -> CheckedVsUnchecked.requirePositive(BigDecimal.ZERO));
        assertEquals("INVALID_PAYMENT", e.code());
        assertFalse(e.retryable());
        assertThrows(NullPointerException.class, () -> CheckedVsUnchecked.requirePositive(null));
        assertThrows(LedgerCorruptedError.class, () -> CheckedVsUnchecked.assertBalanced(BigDecimal.ONE, BigDecimal.TEN));
        CheckedVsUnchecked.assertBalanced(BigDecimal.TEN, new BigDecimal("10.00"));
    }

    @Test
    void catchOrderingAndMultiCatch() {
        assertEquals("ok", CheckedVsUnchecked.classify(() -> { }));
        assertEquals("business:INSUFFICIENT_FUNDS", CheckedVsUnchecked.classify(() -> {
            throw new InsufficientFundsException("A", BigDecimal.ONE, BigDecimal.TEN);
        }));
        assertEquals("payment:PROVIDER_ERROR", CheckedVsUnchecked.classify(() -> {
            throw new PaymentProviderException("x", null, true);
        }));
        assertEquals("runtime:IllegalStateException", CheckedVsUnchecked.classify(() -> {
            throw new IllegalStateException();
        }));
    }
}
