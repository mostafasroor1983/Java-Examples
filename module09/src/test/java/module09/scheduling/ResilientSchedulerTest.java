package module09.scheduling;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResilientSchedulerTest {

    @Test
    void naiveScheduleDiesSilentlyAfterFirstException() throws Exception {
        try (var s = new ResilientScheduler(1, e -> { })) {
            AtomicInteger runs = new AtomicInteger();
            ScheduledFuture<?> f = s.scheduleNaive(() -> {
                if (runs.incrementAndGet() == 2) throw new IllegalStateException("rate provider returned null");
            }, Duration.ofMillis(5));
            var ex = assertThrows(ExecutionException.class, () -> f.get(5, TimeUnit.SECONDS));
            assertInstanceOf(IllegalStateException.class, ex.getCause(), "exception is hidden inside the future");
            int afterFailure = runs.get();
            Thread.sleep(50); // observation window only: proves nothing more runs
            assertEquals(afterFailure, runs.get(), "no further executions: the job is dead");
        }
    }

    @Test
    void guardedScheduleSurvivesFailuresAndReportsThem() throws Exception {
        AtomicInteger reported = new AtomicInteger();
        CountDownLatch tenRuns = new CountDownLatch(10);
        try (var s = new ResilientScheduler(1, e -> reported.incrementAndGet())) {
            AtomicInteger n = new AtomicInteger();
            s.scheduleGuarded("fx-refresh", () -> {
                tenRuns.countDown();
                if (n.incrementAndGet() % 3 == 0) throw new IllegalStateException("transient");
            }, Duration.ofMillis(2));
            assertTrue(tenRuns.await(5, TimeUnit.SECONDS), "keeps running after failures");
            var stats = s.stats("fx-refresh");
            assertTrue(stats.runs() >= 10);
            assertTrue(stats.failures() >= 3);
            assertTrue(reported.get() >= 3);
            assertEquals(new ResilientScheduler.JobStats(0, 0), s.stats("unknown"));
        }
    }
}
