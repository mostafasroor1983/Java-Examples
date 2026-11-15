package module02.exercise1;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class PaymentLedgerTest {

    @Test
    void buggyLedgerLosesPaymentsOnLookup() {
        PaymentLedger ledger = new PaymentLedger();
        PaymentRef paymentRef = new PaymentRef("m1", "o1");
        ledger.record(paymentRef, BigDecimal.TEN);
        PaymentRef lookup = new PaymentRef("m1", "o1");
        assertEquals(new PaymentRef("m1", "o1"), lookup, "equals says they are the same...");
        if (lookup.hashCode() != new PaymentRef("m1", "o1").hashCode()) {
            assertTrue(ledger.find(lookup).isEmpty(), "...but HashMap cannot find it");
        }

        //each PaymentRef object has its own hashCode regaredless the fields of object
        assertEquals(BigDecimal.TEN, ledger.find(paymentRef).get());

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


    @Test
    void stressTest() {
        Map<PaymentRef, BigDecimal> map = new HashMap<>();

        int mises = 0;
        final int COUNT = 10000;
        for (int i = 0; i < COUNT; i++) {
            map.put(new PaymentRef("Key-" + i, "Value-" + i), new BigDecimal(i + ".81"));
        }


        for (int i = 0; i < COUNT; i++) {
            if (Objects.isNull(map.get(new PaymentRef("Key-" + i, "Value-" + i)))) {
                mises++;
            }
        }

        assert  mises == COUNT;
        System.out.println("Mises = " + mises);

    }


}
