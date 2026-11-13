package module15.p12retry;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class P12Test {
    private final List<Duration> sleeps = new ArrayList<>();
    private final P12RetryWithBackoff retry = new P12RetryWithBackoff(4, Duration.ofMillis(100), Duration.ofMillis(500), 2.0,
            e -> e instanceof IllegalStateException, sleeps::add, 42L);

    @Test
    void backoffScheduleIsCappedAndJittered() throws Exception {
        assertEquals(Duration.ofMillis(100), retry.baseDelay(1));
        assertEquals(Duration.ofMillis(200), retry.baseDelay(2));
        assertEquals(Duration.ofMillis(400), retry.baseDelay(3));
        assertEquals(Duration.ofMillis(500), retry.baseDelay(4));
        AtomicInteger calls = new AtomicInteger();
        String r = retry.execute(() -> { if (calls.incrementAndGet() < 3) throw new IllegalStateException("flaky"); return "ok"; });
        assertEquals("ok", r);
        assertEquals(2, sleeps.size());
        assertTrue(sleeps.get(0).toMillis() >= 50 && sleeps.get(0).toMillis() <= 100);
        assertTrue(sleeps.get(1).toMillis() >= 100 && sleeps.get(1).toMillis() <= 200);
    }

    @Test
    void nonRetryableFailsFastAndExhaustionCarriesHistory() {
        AtomicInteger calls = new AtomicInteger();
        var fast = assertThrows(P12RetryWithBackoff.RetriesExhaustedException.class,
                () -> retry.execute(() -> { calls.incrementAndGet(); throw new IllegalArgumentException("bad request"); }));
        assertEquals(1, calls.get());
        assertInstanceOf(IllegalArgumentException.class, fast.getCause());
        calls.set(0);
        var exhausted = assertThrows(P12RetryWithBackoff.RetriesExhaustedException.class,
                () -> retry.execute(() -> { calls.incrementAndGet(); throw new IllegalStateException("down"); }));
        assertEquals(4, calls.get());
        assertEquals(3, exhausted.getSuppressed().length);
        assertThrows(IllegalArgumentException.class, () -> new P12RetryWithBackoff(0, Duration.ZERO, Duration.ZERO, 2, e -> true, d -> { }, 1));
    }
}
