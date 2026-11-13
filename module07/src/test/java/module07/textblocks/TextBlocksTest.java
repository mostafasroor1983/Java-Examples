package module07.textblocks;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TextBlocksTest {

    @Test
    void incidentalIndentationStripped() {
        assertEquals("Account statement\n=================\n", TextBlocks.STATEMENT_HEADER);
    }

    @Test
    void formattedJson() {
        String json = TextBlocks.receiptJson("p1", new BigDecimal("10.50"), "GBP");
        assertEquals("{\n  \"paymentId\": \"p1\",\n  \"amount\": \"10.50\",\n  \"currency\": \"GBP\"\n}\n", json);
        assertEquals("{\n  \"paymentId\": \"p1\"\n}\n", TextBlocks.oldStyle("p1"));
    }

    @Test
    void lineContinuationAndTrailingSpace() {
        assertEquals("SELECT id, amount, currency FROM payments WHERE status = ? ORDER BY created_at DESC", TextBlocks.sqlFindPayments());
    }
}
