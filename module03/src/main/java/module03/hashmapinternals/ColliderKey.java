package module03.hashmapinternals;

import java.util.Objects;

/**
 * Key whose hashCode is constant: every entry lands in the same bucket.
 * Once the bucket has 8+ nodes and table >= 64 the bucket becomes a red-black tree.
 * Because ColliderKey implements Comparable, lookups in the treeified bucket are O(log n)
 * instead of O(n). Without Comparable, HashMap falls back to identity/class-name tiebreaks.
 */
public record ColliderKey(String id) implements Comparable<ColliderKey> {
    public ColliderKey {
        Objects.requireNonNull(id);
    }

    @Override
    public int hashCode() {
        return 42;
    }

    @Override
    public int compareTo(ColliderKey o) {
        return id.compareTo(o.id);
    }
}
