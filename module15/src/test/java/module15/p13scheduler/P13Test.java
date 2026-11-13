package module15.p13scheduler;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class P13Test {
    @Test
    void runsInDueOrderAndSupportsCancelAndPeriodic() {
        AtomicLong now = new AtomicLong();
        var s = new P13DelayedTaskScheduler(now::get);
        List<String> log = new ArrayList<>();
        s.schedule(() -> log.add("b"), Duration.ofMillis(200));
        s.schedule(() -> log.add("a"), Duration.ofMillis(100));
        var cancelled = s.schedule(() -> log.add("never"), Duration.ofMillis(150));
        var periodic = s.scheduleAtFixedRate(() -> log.add("tick"), Duration.ofMillis(100), Duration.ofMillis(100));
        assertEquals(0, s.runDue());
        assertEquals(Duration.ofMillis(100).toNanos(), s.nanosUntilNext());
        assertTrue(cancelled.cancel());
        assertFalse(cancelled.cancel());

        now.set(Duration.ofMillis(100).toNanos());
        assertEquals(2, s.runDue());
        assertEquals(List.of("a", "tick"), log);
        now.set(Duration.ofMillis(250).toNanos());
        assertEquals(2, s.runDue()); // b (200) and tick (200)
        assertEquals(List.of("a", "tick", "b", "tick"), log);
        now.set(Duration.ofMillis(1000).toNanos());
        assertEquals(1, s.runDue(), "fixed-rate catches up one period per runDue call (tick at 300)");
        assertTrue(periodic.cancel());
        assertEquals(0, s.pending());
        assertEquals(-1, s.nanosUntilNext());
        assertThrows(IllegalArgumentException.class, () -> s.schedule(() -> { }, Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> s.scheduleAtFixedRate(() -> { }, Duration.ZERO, Duration.ZERO));
    }
}
