package module15.p08lru;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class P08Test {
    @Test
    void evictsLeastRecentlyUsed() {
        var c = new P08LruCache<String, Integer>(2);
        c.put("a", 1);
        c.put("b", 2);
        assertEquals(1, c.get("a").orElseThrow());
        c.put("c", 3); // evicts b
        assertTrue(c.get("b").isEmpty());
        assertEquals(List.of("c", "a"), c.keysMruFirst());
        c.put("a", 10); // update moves to front
        assertEquals(List.of("a", "c"), c.keysMruFirst());
        assertEquals(10, c.get("a").orElseThrow());
        assertTrue(c.remove("c"));
        assertFalse(c.remove("c"));
        assertEquals(1, c.size());
        assertEquals(2, c.hits());
        assertEquals(1, c.misses());
        assertThrows(IllegalArgumentException.class, () -> new P08LruCache<>(0));
    }
}
