package module03.hashmapinternals;

/**
 * What HashMap does with a key (JDK 8+):
 * 1. h = key.hashCode()
 * 2. spread: h ^ (h >>> 16)   (mix high bits into low bits because index uses only low bits)
 * 3. index = (n - 1) & hash   (n = table length, always a power of two)
 * 4. bucket: linked list; converted to a red-black tree when a bucket has >= 8 nodes and n >= 64
 * 5. resize when size > capacity * 0.75 (default load factor); doubles the table, splits buckets
 */
public final class HashMapInternals {
    public static final int TREEIFY_THRESHOLD = 8;
    public static final int MIN_TREEIFY_CAPACITY = 64;
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private HashMapInternals() {
    }

    public static int spread(int h) {
        return h ^ (h >>> 16);
    }

    public static int bucketIndex(Object key, int tableLength) {
        if (Integer.bitCount(tableLength) != 1) {
            throw new IllegalArgumentException("table length must be a power of two");
        }
        int h = key == null ? 0 : spread(key.hashCode());
        return (tableLength - 1) & h;
    }

    /** Smallest power of two >= requested capacity (what HashMap.tableSizeFor does). */
    public static int tableSizeFor(int cap) {
        if (cap <= 0) throw new IllegalArgumentException("cap must be positive");
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return n < 0 ? 1 : n + 1; // n < 0 only when cap == 1 (shift by 32 is a no-op)
    }

    /** Initial capacity you should pass to hold `expected` entries without a resize. */
    public static int capacityFor(int expected) {
        return tableSizeFor((int) Math.ceil(expected / DEFAULT_LOAD_FACTOR));
    }

    /** Threshold at which a table of this length resizes. */
    public static int resizeThreshold(int tableLength) {
        return (int) (tableLength * DEFAULT_LOAD_FACTOR);
    }
}
