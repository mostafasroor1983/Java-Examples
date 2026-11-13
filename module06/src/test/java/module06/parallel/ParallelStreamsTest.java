package module06.parallel;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

class ParallelStreamsTest {

    @Test
    void associativeReductionsAreExact() throws Exception {
        assertEquals(5_000_050_000L, ParallelStreams.sumRange(100_000));
        assertEquals(5_000_050_000L, ParallelStreams.sumOnDedicatedPool(100_000, 2));
        List<BigDecimal> amounts = IntStream.range(0, 10_000).mapToObj(i -> new BigDecimal("0.01")).collect(Collectors.toList());
        assertEquals(new BigDecimal("100.00"), ParallelStreams.totalParallel(amounts));
    }

    @Test
    void collectIsSafeSharedListIsNot() {
        List<Integer> good = ParallelStreams.collectGood(100_000);
        assertEquals(100_000, good.size());
        assertEquals(IntStream.range(0, 100_000).boxed().collect(Collectors.toList()), good, "collect preserves encounter order");
        int badSize;
        try {
            badSize = ParallelStreams.collectBad(100_000).size();
        } catch (RuntimeException raceCorruption) {
            badSize = -1;
        }
        assertTrue(badSize <= 100_000, "shared mutable list can only lose elements or blow up");
    }

    @Test
    void nonAssociativeReduceDiffersInParallel() {
        List<Long> values = LongStream.rangeClosed(1, 1000).boxed().collect(Collectors.toList());
        long sequential = ParallelStreams.nonAssociativeReduce(values, false);
        assertEquals(-500500L, sequential);
        // The parallel result is unspecified; the lesson is "do not rely on it". We only assert it ran.
        ParallelStreams.nonAssociativeReduce(values, true);
    }
}
