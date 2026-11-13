package com.revolut.buildit.urlshortener;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class UrlShortenerTest {
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
    private final UrlShortener s = new UrlShortener(now::get, Duration.ofDays(30));

    @Test
    void base62RoundTrip() {
        assertThat(UrlShortener.base62(0)).isEqualTo("0");
        assertThat(UrlShortener.base62(61)).isEqualTo("Z");
        assertThat(UrlShortener.base62(62)).isEqualTo("10");
        for (long n : new long[]{1, 100_001, 123_456_789L, Long.MAX_VALUE}) assertThat(UrlShortener.fromBase62(UrlShortener.base62(n))).isEqualTo(n);
        assertThatThrownBy(() -> UrlShortener.fromBase62("a-b")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shortenResolveCountExpire() {
        String code = s.shorten("https://revolut.com/careers");
        assertThat(code).hasSizeGreaterThanOrEqualTo(3);
        assertThat(s.shorten("https://revolut.com/careers")).as("idempotent").isEqualTo(code);
        assertThat(s.shorten("https://revolut.com/other")).isNotEqualTo(code);
        assertThat(s.resolve(code)).contains("https://revolut.com/careers");
        s.resolve(code);
        assertThat(s.stats(code).get().clickCount()).isEqualTo(2);
        now.set(Instant.EPOCH.plus(Duration.ofDays(31)));
        assertThat(s.resolve(code)).isEmpty();
        assertThat(s.stats(code)).isEmpty();
        assertThat(s.shorten("https://revolut.com/careers")).as("new code after expiry").isNotEqualTo(code);
    }

    @Test
    void aliasesAndValidation() {
        assertThat(s.shortenWithAlias("promo-2024", "https://revolut.com/promo", Duration.ofDays(1))).isTrue();
        assertThat(s.shortenWithAlias("promo-2024", "https://x.com", Duration.ofDays(1))).as("alias taken").isFalse();
        assertThat(s.resolve("promo-2024")).contains("https://revolut.com/promo");
        assertThatThrownBy(() -> s.shortenWithAlias("a", "https://x.com", Duration.ofDays(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.shorten("javascript:alert(1)")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.shorten("ftp://x.com/f")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.shorten("not a url")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.shorten("https://x.com/" + "a".repeat(3000))).isInstanceOf(IllegalArgumentException.class);
        assertThat(s.resolve("missing")).isEmpty();
    }

    @Test
    void concurrentShortenOfSameUrlYieldsOneCode() throws Exception {
        Set<String> codes = ConcurrentHashMap.newKeySet();
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) new Thread(() -> { try { start.await(); codes.add(s.shorten("https://revolut.com/same")); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } finally { done.countDown(); } }).start();
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(codes).hasSize(1);
        assertThat(s.size()).isEqualTo(1);
    }
}
