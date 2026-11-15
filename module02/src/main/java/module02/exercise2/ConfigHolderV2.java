package module02.exercise2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * BAD: "immutable" config that leaks. The constructor keeps the caller's map, the getter returns
 * the internal map, and unmodifiableMap is only a view - the caller still holds the backing map.
 */
public class ConfigHolderV2 {
    private final Map<String, String> settings;

    public ConfigHolderV2(Map<String, String> settings) {
        this.settings = settings; // alias to caller's mutable map
    }

    public Map<String, String> getSettings() {
        return settings; // exposes internals
    }

    public Map<String, String> getSettingsView() {
        return Collections.unmodifiableMap(settings); // still tracks the caller's mutations
    }

    public static ConfigHolderV2 defaults() {
        Map<String, String> m = new HashMap<>();
        m.put("fx.spread.bps", "25");
        return new ConfigHolderV2(m);
    }

}
