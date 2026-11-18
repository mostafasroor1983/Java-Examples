package module07.messages;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CreditTransferMessageTest {
    private static CreditTransferMessage.Transfer transfer(String debtor, String amount) {
        return new CreditTransferMessage.Transfer("E2E-2024-0001", "EUR", new BigDecimal(amount),
                LocalDate.of(2024, 3, 1), debtor, "ACME GmbH", "Invoice 42");
    }

    @Test
    void rendersPlainAmountsAndFields() {
        String xml = CreditTransferMessage.render(transfer("Jane Doe", "1000.00"));
        assertTrue(xml.contains("<IntrBkSttlmAmt Ccy=\"EUR\">1000.00</IntrBkSttlmAmt>"));
        assertTrue(xml.contains("<Dbtr><Nm>Jane Doe</Nm></Dbtr>"));
        assertTrue(xml.endsWith("</CdtTrfTxInf>\n"), "text block keeps the trailing newline");
        assertEquals("1000", new BigDecimal("1E+3").toPlainString(), "why toPlainString matters");
    }

    @Test
    void injectionAttemptIsEscaped() {
        String xml = CreditTransferMessage.render(transfer("</Nm></Dbtr><Cdtr><Nm>Mallory", "1.00"));
        assertFalse(xml.contains("<Nm>Mallory"));
        assertTrue(xml.contains("&lt;/Nm&gt;&lt;/Dbtr&gt;"));
        assertEquals(1, xml.split("<Cdtr>", -1).length - 1, "exactly one creditor element");
    }

    @Test
    void controlCharsDroppedAndLengthTruncated() {
        assertEquals("ab", CreditTransferMessage.xml("a\u0000b", 140));
        assertEquals(140, CreditTransferMessage.xml("x".repeat(500), 140).length());
        assertEquals("", CreditTransferMessage.xml(null, 140));
    }

    @Test
    void invalidTransfersRejected() {
        assertThrows(IllegalArgumentException.class, () -> transfer("a", "1.001"));
        assertThrows(IllegalArgumentException.class, () -> transfer("a", "0"));
        assertThrows(IllegalArgumentException.class, () -> new CreditTransferMessage.Transfer("bad id!", "EUR",
                BigDecimal.ONE, LocalDate.now(), "a", "b", "c"));
    }
}
