package module02.exercise2;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigHolderTest {

    @Test
    void badHolderLeaksThroughAliasAndGetter() {
        Map<String, String> source = new HashMap<>(Map.of("fx.spread.bps", "25"));
        ConfigHolder holder = new ConfigHolder(source);
        source.put("fx.spread.bps", "9999");          // alias leak
        assertEquals("9999", holder.getSettings().get("fx.spread.bps"));
        holder.getSettings().put("hack", "1");         // getter leak
        assertEquals("1", holder.getSettingsView().get("hack"));
        assertThrows(UnsupportedOperationException.class, () -> holder.getSettingsView().put("x", "y"));
        source.put("fx.spread.bps", "1");               // view still tracks the alias
        assertEquals("1", holder.getSettingsView().get("fx.spread.bps"));
    }

    @Test
    void immutableConfigIsIsolated() {
        Map<String, String> source = new HashMap<>(Map.of("fx.spread.bps", "25"));
        ImmutableConfig cfg = new ImmutableConfig(source);
        source.put("fx.spread.bps", "9999");
        assertEquals(25, cfg.getInt("fx.spread.bps", 0));
        assertThrows(UnsupportedOperationException.class, () -> cfg.asMap().put("x", "y"));
        ImmutableConfig cfg2 = cfg.with("fx.spread.bps", "30");
        assertEquals(25, cfg.getInt("fx.spread.bps", 0));
        assertEquals(30, cfg2.getInt("fx.spread.bps", 0));
        assertEquals(7, cfg.getInt("missing", 7));
        assertTrue(cfg.get("missing").isEmpty());
        assertEquals(cfg, new ImmutableConfig(Map.of("fx.spread.bps", "25")));
    }

    @Test
    void immutableConfigRejectsNulls() {
        Map<String, String> withNull = new HashMap<>();
        withNull.put("k", null);
        assertThrows(NullPointerException.class, () -> new ImmutableConfig(withNull));
        assertThrows(NullPointerException.class, () -> new ImmutableConfig(Map.of()).with("k", null));
    }
}
