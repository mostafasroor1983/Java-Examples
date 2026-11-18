package module02.defensive;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SignedSettlementBatchTest {
    private static final Instant CUT_OFF = Instant.parse("2024-03-01T16:00:00Z");

    @Test
    void mutatingInputsAfterConstructionHasNoEffect() {
        List<String> refs = new ArrayList<>(List.of("PAY-1", "PAY-2"));
        byte[] sig = {1, 2, 3, 4, 5};
        var batch = new SignedSettlementBatch("B-1", refs, CUT_OFF, sig);
        refs.add("PAY-EVIL");
        sig[0] = 99;
        assertEquals(List.of("PAY-1", "PAY-2"), batch.paymentRefs());
        assertEquals(1, batch.signature()[0]);
    }

    @Test
    void accessorsCannotBeUsedToTamper() {
        var batch = new SignedSettlementBatch("B-1", List.of("PAY-1"), CUT_OFF, new byte[]{1, 2});
        batch.signature()[0] = 42;
        assertEquals(1, batch.signature()[0]);
        assertThrows(UnsupportedOperationException.class, () -> batch.paymentRefs().add("X"));
    }

    @Test
    void equalsComparesArrayContentNotReference() {
        var a = new SignedSettlementBatch("B-1", List.of("PAY-1"), CUT_OFF, new byte[]{1, 2});
        var b = new SignedSettlementBatch("B-1", List.of("PAY-1"), CUT_OFF, new byte[]{1, 2});
        assertEquals(a, b);
        Set<SignedSettlementBatch> set = new HashSet<>(List.of(a));
        assertTrue(set.contains(b));
        assertNotEquals(a, new SignedSettlementBatch("B-1", List.of("PAY-1"), CUT_OFF, new byte[]{1, 3}));
        assertTrue(a.toString().startsWith("SignedSettlementBatch[B-1, payments=1"));
    }

    @Test
    void invalidBatchesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SignedSettlementBatch("B", List.of(), CUT_OFF, new byte[1]));
        assertThrows(IllegalArgumentException.class,
                () -> new SignedSettlementBatch("B", List.of("P", "P"), CUT_OFF, new byte[1]));
        assertThrows(NullPointerException.class,
                () -> new SignedSettlementBatch("B", java.util.Arrays.asList("P", null), CUT_OFF, new byte[1]));
    }
}
