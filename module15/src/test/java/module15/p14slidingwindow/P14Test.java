package module15.p14slidingwindow;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class P14Test {
    @Test
    void slidesOutOldBuckets() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        var w = new P14SlidingWindowStats(Duration.ofMinutes(10), 10, now::get); // 1-minute buckets
        w.record(new BigDecimal("10"));
        now.set(Instant.EPOCH.plus(Duration.ofMinutes(5)));
        w.record(new BigDecimal("20"));
        assertEquals(2, w.count());
        assertEquals(new BigDecimal("30"), w.sum());
        now.set(Instant.EPOCH.plus(Duration.ofMinutes(9)).plusSeconds(59));
        assertEquals(2, w.count(), "first record still inside the 10-minute window");
        now.set(Instant.EPOCH.plus(Duration.ofMinutes(10)));
        assertEquals(1, w.count(), "bucket 0 has slid out");
        assertEquals(new BigDecimal("20"), w.sum());
        now.set(Instant.EPOCH.plus(Duration.ofMinutes(15)));
        assertEquals(0, w.count());
        w.record(BigDecimal.ONE); // reuses the stale slot safely
        assertEquals(1, w.count());
        assertThrows(IllegalArgumentException.class, () -> new P14SlidingWindowStats(Duration.ofMinutes(10), 7, now::get));
    }
}
