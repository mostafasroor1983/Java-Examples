package module02.exercise1;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentLedgerTest {

    @Test
    void buggyLedgerLosesPaymentsOnLookup() {
        PaymentLedger ledger = new PaymentLedger();
        ledger.record(new PaymentRef("m1", "o1"), BigDecimal.TEN);
        PaymentRef lookup = new PaymentRef("m1", "o1");
        assertEquals(new PaymentRef("m1", "o1"), lookup, "equals says they are the same...");
        if (lookup.hashCode() != new PaymentRef("m1", "o1").hashCode()) {
            assertTrue(ledger.find(lookup).isEmpty(), "...but HashMap cannot find it");
        }
        // and duplicates pile up
        ledger.record(new PaymentRef("m1", "o1"), BigDecimal.TEN);
        assertEquals(2, ledger.size());
    }

    @Test
    void fixedLedgerIsIdempotentAndFindable() {
        PaymentLedgerFixed ledger = new PaymentLedgerFixed();
        assertTrue(ledger.record(new PaymentRefFixed("m1", "o1"), BigDecimal.TEN));
        assertFalse(ledger.record(new PaymentRefFixed("m1", "o1"), BigDecimal.TEN), "duplicate ignored");
        assertTrue(ledger.record(new PaymentRefFixed("m1", "o2"), BigDecimal.ONE));
        assertEquals(BigDecimal.TEN, ledger.find(new PaymentRefFixed("m1", "o1")).orElseThrow());
        assertEquals(2, ledger.size());
        assertEquals(new BigDecimal("11"), ledger.total());
    }
}
