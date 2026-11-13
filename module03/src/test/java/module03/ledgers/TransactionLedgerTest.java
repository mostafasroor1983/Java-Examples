package module03.ledgers;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TransactionLedgerTest {

    @Test
    void indexesAgree() {
        TransactionLedger l = new TransactionLedger();
        l.append(new TransactionLedger.Entry("1", "A", new BigDecimal("100"), Instant.ofEpochSecond(10)));
        l.append(new TransactionLedger.Entry("2", "A", new BigDecimal("-30"), Instant.ofEpochSecond(20)));
        l.append(new TransactionLedger.Entry("3", "B", new BigDecimal("5"), Instant.ofEpochSecond(20)));
        assertEquals(new BigDecimal("70"), l.balance("A"));
        assertEquals(new BigDecimal("5"), l.balance("B"));
        assertEquals(BigDecimal.ZERO, l.balance("Z"));
        assertEquals(2, l.forAccount("A").size());
        assertEquals(2, l.between(Instant.ofEpochSecond(15), Instant.ofEpochSecond(21)).size());
        assertEquals(1, l.between(Instant.ofEpochSecond(0), Instant.ofEpochSecond(20)).size());
        assertTrue(l.balancesReconcile());
        assertEquals(3, l.size());
    }

    @Test
    void rejectsZeroAmountAndExposesReadOnlyViews() {
        TransactionLedger l = new TransactionLedger();
        assertThrows(IllegalArgumentException.class,
                () -> new TransactionLedger.Entry("1", "A", BigDecimal.ZERO, Instant.EPOCH));
        l.append(new TransactionLedger.Entry("1", "A", BigDecimal.ONE, Instant.EPOCH));
        assertThrows(UnsupportedOperationException.class, () -> l.forAccount("A").clear());
    }
}
