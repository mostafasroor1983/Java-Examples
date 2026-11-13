package module04.races;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LostUpdateDemoTest {
    private static final int THREADS = 8, ITERATIONS = 20_000;

    @Test
    void unsafeAndCompoundVariantsCannotExceedExpected() throws Exception {
        long expected = (long) THREADS * ITERATIONS;
        assertTrue(LostUpdateDemo.runUnsafe(THREADS, ITERATIONS) <= expected);
        assertTrue(LostUpdateDemo.runCompoundOnChm(THREADS, ITERATIONS) <= expected);
    }

    @Test
    void mergeAndLongAdderAreExact() throws Exception {
        long expected = (long) THREADS * ITERATIONS;
        assertEquals(expected, LostUpdateDemo.runMerge(THREADS, ITERATIONS));
        assertEquals(expected, LostUpdateDemo.runLongAdder(THREADS, ITERATIONS));
    }
}
