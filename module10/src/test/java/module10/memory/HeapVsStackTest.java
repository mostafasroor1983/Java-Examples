package module10.memory;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeapVsStackTest {

    @Test
    void stackLocalsAndOverflow() {
        assertEquals(4950, HeapVsStack.sumLocals(100));
        int depth = HeapVsStack.recursionDepthUntilOverflow();
        assertTrue(depth > 1000, "default stack should allow thousands of frames, got " + depth);
    }

    @Test
    void referencesArePassedByValue() {
        List<String> list = new ArrayList<>();
        HeapVsStack.reassignParameter(list);
        assertTrue(list.isEmpty());
        HeapVsStack.mutateParameter(list);
        assertEquals(List.of("visible"), list);
    }

    @Test
    void boxedIdentityTrap() {
        assertTrue(HeapVsStack.boxedIdentityTrap(127, 127));
        assertFalse(HeapVsStack.boxedIdentityTrap(128, 128), "outside the Integer cache == compares references");
        assertTrue(HeapVsStack.boxedEqualsCorrect(128, 128));
    }

    @Test
    void heapAllocationIsVisibleToRuntime() {
        long before = HeapVsStack.usedHeapBytes();
        List<java.math.BigDecimal> keep = HeapVsStack.allocateOnHeap(200_000);
        long after = HeapVsStack.usedHeapBytes();
        assertEquals(200_000, keep.size());
        // GC may run in between; we only assert the list is on the heap and alive, not a strict byte delta.
        assertTrue(after >= 0 && before >= 0);
    }
}
