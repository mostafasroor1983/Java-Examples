package module14.boxing;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BoxingDemoTest {

    @Test
    void boxedAndPrimitiveAgree() {
        assertEquals(BoxingDemo.sumPrimitive(10_000), BoxingDemo.sumBoxed(10_000));
        assertEquals(BoxingDemo.sumStreamPrimitive(10_000), BoxingDemo.sumStreamBoxed(10_000));
    }

    @Test
    void identityAndEqualityTraps() {
        assertTrue(BoxingDemo.integerCacheHit(127));
        assertFalse(BoxingDemo.integerCacheHit(128));
        assertFalse(BoxingDemo.longEqualsInt());
        assertThrows(NullPointerException.class, () -> BoxingDemo.unboxNull(Map.of(), "missing"));
        assertEquals(0, BoxingDemo.unboxNullSafe(Map.of(), "missing"));
        assertEquals(List.of(30), BoxingDemo.removeTrap());
    }

    @Test
    void countingVariantsAgree() {
        int[] statuses = {200, 201, 404, 500, 503, 302};
        int[] prim = BoxingDemo.countStatusesPrimitive(statuses);
        Map<Integer, Integer> boxed = BoxingDemo.countStatusesBoxed(statuses);
        assertEquals(2, prim[2]);
        assertEquals(2, boxed.get(2));
        assertEquals(2, prim[5]);
        assertEquals(1, prim[3]);
        assertEquals(1, prim[4]);
    }
}
