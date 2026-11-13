package module03.exercise3;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountCacheTest {
    private static AccountCache.Account acc(String id) {
        return new AccountCache.Account(id, BigDecimal.TEN);
    }

    @Test
    void v1GrowsUnbounded() {
        AccountCache c = new AccountCache();
        for (int i = 0; i < 1000; i++) c.put(acc("a" + i));
        assertEquals(1000, c.size());
        assertTrue(c.get("a0").isPresent());
    }

    @Test
    void v2EvictsLeastRecentlyUsed() {
        AccountCacheV2 c = new AccountCacheV2(2);
        c.put(acc("a"));
        c.put(acc("b"));
        c.get("a");          // a is now most recent
        c.put(acc("c"));     // evicts b
        assertTrue(c.get("a").isPresent());
        assertTrue(c.get("b").isEmpty());
        assertTrue(c.get("c").isPresent());
        assertEquals(2, c.size());
        assertThrows(IllegalArgumentException.class, () -> new AccountCacheV2(0));
    }

    @Test
    void v3BehavesLikeV2AndUpdatesInPlace() {
        AccountCacheV3 c = new AccountCacheV3(2);
        c.put(acc("a"));
        c.put(acc("b"));
        c.get("a");
        c.put(acc("c"));
        assertTrue(c.get("a").isPresent());
        assertTrue(c.get("b").isEmpty());
        c.put(new AccountCache.Account("a", BigDecimal.ONE)); // update existing, no eviction
        assertEquals(BigDecimal.ONE, c.get("a").orElseThrow().balance());
        assertEquals(2, c.size());
        assertTrue(c.get("c").isPresent());
    }
}
