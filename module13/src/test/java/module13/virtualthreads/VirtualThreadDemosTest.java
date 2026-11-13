package module13.virtualthreads;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadDemosTest {

    @Test
    void tenThousandBlockingTasksComplete() throws Exception {
        long start = System.nanoTime();
        assertEquals(10_000, VirtualThreadDemos.fanOutBlockingCalls(10_000, Duration.ofMillis(50)));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertTrue(elapsedMs < 10_000, "10k x 50ms sleeps must overlap, took " + elapsedMs + "ms");
    }

    @Test
    void fetchBalancesPreservesOrder() throws Exception {
        var balances = VirtualThreadDemos.fetchBalances(List.of("A", "B", "C"), id -> new BigDecimal(id.charAt(0) - 'A'));
        assertEquals(List.of(new BigDecimal("0"), new BigDecimal("1"), new BigDecimal("2")), balances);
    }

    @Test
    void builderAndIdentity() throws Exception {
        assertEquals("settlement-vt-1", VirtualThreadDemos.nameOfVirtualThread());
        assertTrue(VirtualThreadDemos.isVirtual(Thread.ofVirtual().unstarted(() -> { })));
        assertFalse(VirtualThreadDemos.isVirtual(Thread.currentThread()));
        long carriers = VirtualThreadDemos.distinctCarriersObserved(2000);
        assertTrue(carriers <= Runtime.getRuntime().availableProcessors() + 2, "carriers observed " + carriers);
        assertTrue(VirtualThreadDemos.nanosToRun(200, true) >= 0 && VirtualThreadDemos.nanosToRun(200, false) >= 0);
    }
}
