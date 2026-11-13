package module03.hashmapinternals;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HashMapInternalsTest {

    @Test
    void tableSizeIsNextPowerOfTwo() {
        assertEquals(1, HashMapInternals.tableSizeFor(1));
        assertEquals(16, HashMapInternals.tableSizeFor(13));
        assertEquals(16, HashMapInternals.tableSizeFor(16));
        assertEquals(32, HashMapInternals.tableSizeFor(17));
        assertThrows(IllegalArgumentException.class, () -> HashMapInternals.tableSizeFor(0));
    }

    @Test
    void capacityForAvoidsResize() {
        // 100 entries / 0.75 = 133.3 -> 256 table; threshold 192 >= 100
        assertEquals(256, HashMapInternals.capacityFor(100));
        assertTrue(HashMapInternals.resizeThreshold(HashMapInternals.capacityFor(100)) >= 100);
        assertEquals(12, HashMapInternals.resizeThreshold(16));
    }

    @Test
    void bucketIndexUsesLowBitsOfSpreadHash() {
        assertEquals(0, HashMapInternals.bucketIndex(null, 16));
        int idx = HashMapInternals.bucketIndex("GBP", 16);
        assertTrue(idx >= 0 && idx < 16);
        assertEquals(idx, HashMapInternals.bucketIndex("GBP", 16));
        assertThrows(IllegalArgumentException.class, () -> HashMapInternals.bucketIndex("x", 15));
    }

    @Test
    void collidingKeysStillWorkCorrectlyAfterTreeify() {
        Map<ColliderKey, Integer> map = new HashMap<>(128);
        for (int i = 0; i < 100; i++) {
            map.put(new ColliderKey("k" + i), i);
        }
        assertEquals(100, map.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i, map.get(new ColliderKey("k" + i)));
        }
        assertNull(map.get(new ColliderKey("missing")));
        assertEquals(50, map.remove(new ColliderKey("k50")));
        assertEquals(99, map.size());
    }
}
