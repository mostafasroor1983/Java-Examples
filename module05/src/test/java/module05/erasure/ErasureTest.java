package module05.erasure;

import module05.bounds.Money;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErasureTest {

    @Test
    void genericsAreErasedAtRuntime() {
        assertTrue(Erasure.sameRuntimeClass(new ArrayList<>(), new ArrayList<>()));
    }

    @Test
    void heapPollutionFailsAtReadSiteNotWriteSite() {
        List<String> polluted = Erasure.pollute(); // no exception here
        assertEquals(1, polluted.size());
        assertThrows(ClassCastException.class, () -> {
            String s = polluted.get(0); // checkcast inserted by the compiler
            fail("unreachable " + s);
        });
    }

    @Test
    void typeTokensSurviveErasure() {
        String[] arr = Erasure.newArray(String.class, 3);
        assertEquals(3, arr.length);
        Erasure.Favorites f = new Erasure.Favorites();
        f.put(String.class, "GBP");
        f.put(Integer.class, 42);
        assertEquals("GBP", f.get(String.class));
        assertEquals(42, f.get(Integer.class));
        assertNull(f.get(Double.class));
    }

    @Test
    void describeAndBridgeMethods() {
        assertEquals("strings", Erasure.describe(List.of("a")));
        assertEquals("numbers", Erasure.describe(List.of(1)));
        assertEquals("empty", Erasure.describe(List.of()));
        assertTrue(Erasure.hasBridgeMethod(Money.class, "compareTo"), "Comparable<Money> yields a synthetic compareTo(Object)");
    }
}
