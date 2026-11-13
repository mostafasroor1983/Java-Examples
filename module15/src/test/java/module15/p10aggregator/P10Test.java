package module15.p10aggregator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class P10Test {
    @Test
    void statsPerMerchantAndOverall() throws Exception {
        var agg = new P10TransactionAggregator();
        int threads = 8, per = 1000;
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            String m = "m" + (t % 2);
            new Thread(() -> { try { for (int i = 1; i <= per; i++) agg.record(m, BigDecimal.valueOf(i)); } finally { done.countDown(); } }).start();
        }
        assertTrue(done.await(10, TimeUnit.SECONDS));
        var m0 = agg.stats("m0");
        assertEquals(4 * per, m0.count());
        assertEquals(BigDecimal.valueOf(4L * per * (per + 1) / 2), m0.sum());
        assertEquals(BigDecimal.ONE, m0.min());
        assertEquals(BigDecimal.valueOf(per), m0.max());
        assertEquals(new BigDecimal("500.50"), m0.average());
        assertEquals(8 * per, agg.overall().count());
        assertEquals(0, agg.stats("none").count());
        assertEquals(BigDecimal.ZERO, agg.stats("none").average());
        assertThrows(IllegalArgumentException.class, () -> agg.record("x", new BigDecimal("-1")));
        assertEquals(2, agg.snapshot().size());
    }
}
