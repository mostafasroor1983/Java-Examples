package module11.livelock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LivelockTest {

    @Test
    void tieBreakGuaranteesProgress() throws Exception {
        assertEquals(2, LivelockDemo.tieBreakGood(100_000));
    }

    @Test
    void politeWorkersMayStarve() throws Exception {
        // Livelock is probabilistic; we only assert the run terminates (bounded rounds) and never exceeds the work count.
        int completed = LivelockDemo.politeWorkersBad(1_000);
        assertTrue(completed >= 0 && completed <= 2);
    }
}
