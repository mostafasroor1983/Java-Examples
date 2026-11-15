package module02.exercise2;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * GOOD: copies in, exposes an unmodifiable immutable copy, builder for changes.
 */
public final class ImmutableConfig {
    private final Map<String, String> settings;

    public ImmutableConfig(Map<String, String> settings) {
        this.settings = Map.copyOf(Objects.requireNonNull(settings, "settings"));
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(settings.get(key));
    }

    public int getInt(String key, int defaultValue) {
        return get(key).map(Integer::parseInt)
                .orElse(defaultValue);
    }

    public Map<String, String> asMap() {
        return settings; // Map.copyOf result is already immutable
    }

    public ImmutableConfig with(String key, String value) {
        //In here we created a new instance using new, not just creating a new reference
        //var copy = settings; doesn't work becuse every change need a new instance.
        var copy = new java.util.HashMap<>(settings);

        copy.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
        return new ImmutableConfig(copy);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ImmutableConfig immutableConfig)
                && settings.equals(immutableConfig.settings);
    }

    @Override
    public int hashCode() {
        return settings.hashCode();
    }
}
