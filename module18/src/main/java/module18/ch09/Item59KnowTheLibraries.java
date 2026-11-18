package module18.ch09;

import java.util.HexFormat;
import java.util.random.RandomGenerator;

/**
 * Item 59 - Know and use the libraries.
 *
 * <p>Hand-rolled code has subtle bugs the JDK already fixed. {@code Math.abs(rnd.nextInt()) % n} is biased and can
 * even be negative ({@code Math.abs(Integer.MIN_VALUE)} is negative). Use {@code nextInt(bound)}, {@code Math.floorMod},
 * {@link HexFormat} (Java 17), {@code String.strip/isBlank/repeat} (11), {@code Collectors.teeing} (12),
 * {@code Stream.toList} (16), the {@link RandomGenerator} API (17), sequenced collections (21). Know java.lang,
 * java.util (incl. concurrent and function) and java.io/nio.
 */
public final class Item59KnowTheLibraries {
    private Item59KnowTheLibraries() {
    }

    /** BAD: can return a negative shard for Integer.MIN_VALUE hash codes. */
    public static int shardBad(int hash, int shards) {
        return Math.abs(hash) % shards;
    }

    /** GOOD */
    public static int shard(int hash, int shards) {
        return Math.floorMod(hash, shards);
    }

    public static int pickProvider(RandomGenerator rnd, int providers) {
        return rnd.nextInt(providers); // unbiased, never negative
    }

    public static String hex(byte[] digest) {
        return HexFormat.of().formatHex(digest);
    }
}
