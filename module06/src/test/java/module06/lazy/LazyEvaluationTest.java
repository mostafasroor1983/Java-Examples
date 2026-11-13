package module06.lazy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LazyEvaluationTest {

    @Test
    void elementsFlowVerticallyAndStopEarly() {
        List<String> log = LazyEvaluation.trace(List.of(1, 2, 3, 4));
        assertEquals(List.of("pipeline built, nothing executed yet", "filter 1", "filter 2", "map 2", "found 20"), log);
    }

    @Test
    void infiniteStreamBoundedByLimit() {
        assertEquals(List.of(0L, 1L, 1L, 2L, 3L, 5L, 8L), LazyEvaluation.firstNFibonacci(7));
    }

    @Test
    void sortedIsABarrier() {
        assertEquals(5, LazyEvaluation.elementsVisitedBeforeFirstAfterSort(List.of(5, 4, 3, 2, 1)));
    }

    @Test
    void streamsAreSingleUse() {
        assertFalse(LazyEvaluation.canReuseStream(Stream.of(1, 2, 3)));
    }
}
