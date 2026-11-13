package com.revolut.buildit.urlshortener;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * Build It #6: URL shortener. Base62 of a monotonically increasing id (no collisions, no retries),
 * custom aliases, expiry, click counting, same long URL -> same code (idempotent create).
 */
public final class UrlShortener {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public record Entry(String code, String longUrl, Instant createdAt, Instant expiresAt, LongAdder clicks) {
        public long clickCount() {
            return clicks.sum();
        }
    }

    private final ConcurrentHashMap<String, Entry> byCode = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> codeByUrl = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(100_000); // start high so codes are >= 3 chars
    private final Supplier<Instant> clock;
    private final Duration defaultTtl;

    public UrlShortener(Supplier<Instant> clock, Duration defaultTtl) {
        this.clock = Objects.requireNonNull(clock);
        this.defaultTtl = Objects.requireNonNull(defaultTtl);
    }

    public static String base62(long n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        if (n == 0) return "0";
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            sb.append(ALPHABET.charAt((int) (n % 62)));
            n /= 62;
        }
        return sb.reverse().toString();
    }

    public static long fromBase62(String s) {
        long n = 0;
        for (char c : s.toCharArray()) {
            int d = ALPHABET.indexOf(c);
            if (d < 0) throw new IllegalArgumentException("invalid base62 char " + c);
            n = n * 62 + d;
        }
        return n;
    }

    /** Same URL returns the same code while it is alive. */
    public String shorten(String longUrl) {
        validate(longUrl);
        String existing = codeByUrl.get(longUrl);
        if (existing != null && byCode.containsKey(existing)) return existing;
        String code = base62(sequence.incrementAndGet());
        Instant now = clock.get();
        byCode.put(code, new Entry(code, longUrl, now, now.plus(defaultTtl), new LongAdder()));
        String raced = codeByUrl.putIfAbsent(longUrl, code);
        if (raced != null && byCode.containsKey(raced)) {
            byCode.remove(code); // someone else won; use theirs
            return raced;
        }
        codeByUrl.put(longUrl, code);
        return code;
    }

    public boolean shortenWithAlias(String alias, String longUrl, Duration ttl) {
        validate(longUrl);
        if (alias == null || !alias.matches("[0-9A-Za-z_-]{3,32}")) throw new IllegalArgumentException("alias must be 3-32 url-safe chars");
        Instant now = clock.get();
        boolean created = byCode.putIfAbsent(alias, new Entry(alias, longUrl, now, now.plus(ttl), new LongAdder())) == null;
        if (created) codeByUrl.putIfAbsent(longUrl, alias);
        return created;
    }

    /** Resolve and count a click; expired entries resolve to empty and are removed lazily. */
    public Optional<String> resolve(String code) {
        Entry e = byCode.get(code);
        if (e == null) return Optional.empty();
        if (!e.expiresAt().isAfter(clock.get())) {
            byCode.remove(code, e);
            codeByUrl.remove(e.longUrl(), code);
            return Optional.empty();
        }
        e.clicks().increment();
        return Optional.of(e.longUrl());
    }

    public Optional<Entry> stats(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    public int size() {
        return byCode.size();
    }

    private static void validate(String longUrl) {
        Objects.requireNonNull(longUrl);
        if (longUrl.length() > 2048) throw new IllegalArgumentException("url too long");
        URI uri;
        try {
            uri = URI.create(longUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid url", e);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
            throw new IllegalArgumentException("only absolute http(s) urls are accepted");
        }
    }
}
