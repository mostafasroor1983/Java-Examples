package module09.coordination;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CoordinationTest {

    @Test
    void latchStartsAndAwaitsWorkers() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        assertEquals(10, Coordination.startTogetherAndAwait(10, counter::incrementAndGet));
        assertEquals(10, counter.get());
    }

    @Test
    void barrierSeparatesPhases() throws Exception {
        List<String> log = Coordination.phasedSettlement(3, 2);
        int phase1 = log.indexOf("phase 1 complete");
        int phase2 = log.indexOf("phase 2 complete");
        assertTrue(phase1 > 0 && phase2 > phase1);
        // every p0 entry is before "phase 1 complete", every p1 entry after it
        for (int i = 0; i < log.size(); i++) {
            String e = log.get(i);
            if (e.endsWith(" p0")) assertTrue(i < phase1, e);
            if (e.endsWith(" p1")) assertTrue(i > phase1 && i < phase2, e);
        }
        assertEquals(8, log.size());
    }

    @Test
    void semaphoreBoundsConcurrency() throws Exception {
        int max = Coordination.boundedConcurrency(3, 12);
        assertTrue(max <= 3, "never more than 3 in flight, observed " + max);
        assertTrue(max >= 1);
        Semaphore s = new Semaphore(1);
        assertTrue(Coordination.admit(s));
        assertFalse(Coordination.admit(s));
        s.release();
        assertTrue(Coordination.admit(s));
    }
}
