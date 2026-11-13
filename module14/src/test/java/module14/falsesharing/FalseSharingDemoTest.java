package module14.falsesharing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FalseSharingDemoTest {

    @Test
    void bothLayoutsCountCorrectly() throws Exception {
        long n = 200_000;
        var adjacent = new FalseSharingDemo.AdjacentCounters();
        long tAdjacent = FalseSharingDemo.run(() -> adjacent.a++, () -> adjacent.b++, n);
        var padded = new FalseSharingDemo.PaddedCounters();
        long tPadded = FalseSharingDemo.run(() -> padded.a++, () -> padded.b++, n);
        assertEquals(n, adjacent.a);
        assertEquals(n, adjacent.b);
        assertEquals(n, padded.a);
        assertEquals(n, padded.b);
        assertTrue(tAdjacent > 0 && tPadded > 0, "timing is informational; padded is typically faster on multi-core hardware");
    }

    @Test
    void adderAndAtomicAreExact() throws Exception {
        assertEquals(8 * 20_000L, FalseSharingDemo.adderUnderContention(8, 20_000));
        assertEquals(8 * 20_000L, FalseSharingDemo.atomicUnderContention(8, 20_000));
    }
}
