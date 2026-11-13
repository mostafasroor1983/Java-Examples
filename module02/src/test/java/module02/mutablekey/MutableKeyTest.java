package module02.mutablekey;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MutableKeyTest {

    @Test
    void mutatingKeyAfterInsertionOrphansEntry() {
        Map<MutablePaymentKey, BigDecimal> map = new HashMap<>();
        MutablePaymentKey key = new MutablePaymentKey("m1", "o1");
        map.put(key, BigDecimal.TEN);

        key.setOrderId("o2"); // hash changes, entry stays in old bucket

        assertNull(map.get(key), "same reference, not found");
        assertNull(map.get(new MutablePaymentKey("m1", "o1")), "original value, not found either");
        assertNull(map.get(new MutablePaymentKey("m1", "o2")));
        assertEquals(1, map.size(), "entry is still there, just unreachable");
        assertFalse(map.containsKey(key));
    }

    @Test
    void recordKeyIsSafe() {
        Map<PaymentKey, BigDecimal> map = new HashMap<>();
        map.put(new PaymentKey("m1", "o1"), BigDecimal.TEN);
        assertEquals(BigDecimal.TEN, map.get(new PaymentKey("m1", "o1")));
        assertThrows(NullPointerException.class, () -> new PaymentKey(null, "o"));
    }
}
