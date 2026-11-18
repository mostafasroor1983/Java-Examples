package module14.benchmark;

import module14.money.MinorUnitFees;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MiniBenchmarkTest {

    @Test
    void percentilesUseNearestRank() {
        long[] s = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        assertEquals(50, MiniBenchmark.percentile(s, 50));
        assertEquals(100, MiniBenchmark.percentile(s, 99));
        assertEquals(10, MiniBenchmark.percentile(new long[]{10}, 99));
        assertThrows(IllegalArgumentException.class, () -> MiniBenchmark.percentile(new long[0], 50));
    }

    @Test
    void warmupAndMeasurementBothRunAndResultsAreConsumed() {
        AtomicInteger calls = new AtomicInteger();
        var r = new MiniBenchmark().run("count", 50, 200, () -> calls.incrementAndGet());
        assertEquals(250, calls.get());
        assertEquals(200, r.iterations());
        assertTrue(r.p99Nanos() >= r.medianNanos());
    }

    @Test
    void compareMinorUnitsWithBigDecimalWithoutAssertingTimings() {
        long[] minor = new long[10_000];
        BigDecimal[] big = new BigDecimal[minor.length];
        for (int i = 0; i < minor.length; i++) {
            minor[i] = 1_000 + i;
            big[i] = BigDecimal.valueOf(minor[i], 2);
        }
        var bench = new MiniBenchmark();
        var fast = bench.run("minor-units", 20, 50, () -> MinorUnitFees.sumFeesMinor(minor, 145));
        var slow = bench.run("bigdecimal", 20, 50,
                () -> MinorUnitFees.sumFeesBigDecimal(big, new BigDecimal("1.45")).unscaledValue().longValue());
        // Correctness only. Print timings for the curious; never assert them in CI.
        System.out.printf("minor-units median %,d ns | BigDecimal median %,d ns%n", fast.medianNanos(), slow.medianNanos());
        assertTrue(fast.medianNanos() > 0 && slow.medianNanos() > 0);
    }
}
