package module14.allocation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AllocationProbeTest {

    @Test
    void retainedBigDecimalsAllocateFarMoreThanAPrimitiveArray() {
        assumeTrue(AllocationProbe.supported(), "HotSpot allocation counters not available");
        int n = 100_000;
        List<BigDecimal> keep = new ArrayList<>(n);
        long[] cents = new long[n];
        long boxed = AllocationProbe.allocatedBytes(() -> {
            for (int i = 0; i < n; i++) keep.add(BigDecimal.valueOf(1_000_000L + i, 2)); // escapes: cannot be eliminated
        });
        long primitive = AllocationProbe.allocatedBytes(() -> {
            for (int i = 0; i < n; i++) cents[i] = 1_000_000L + i; // writes into a pre-allocated array
        });
        assertEquals(n, keep.size());
        assertTrue(boxed > (long) n * 16, "each BigDecimal is a heap object: " + boxed + " bytes");
        assertTrue(primitive < boxed / 10, "primitive loop allocates (almost) nothing: " + primitive + " bytes");
    }
}
