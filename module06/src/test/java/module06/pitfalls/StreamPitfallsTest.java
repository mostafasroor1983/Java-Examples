package module06.pitfalls;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class StreamPitfallsTest {

    @Test
    void doubleMoneyIsWrongBigDecimalIsRight() {
        List<String> amounts = List.of("0.10", "0.20");
        assertNotEquals(0.30, StreamPitfalls.totalWithDoubleBad(amounts));
        assertEquals(new BigDecimal("0.30"), StreamPitfalls.totalWithBigDecimal(amounts));
    }

    @Test
    void toMapRejectsNullValues() {
        assertThrows(NullPointerException.class, () -> StreamPitfalls.toMapWithNullsBad(List.of("a", "xb")));
    }

    @Test
    void optionalGetVsOrElseThrow() {
        assertThrows(NoSuchElementException.class, () -> StreamPitfalls.firstIdBad(List.of()));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> StreamPitfalls.firstIdGood(List.of()));
        assertEquals("no ids", e.getMessage());
    }

    @Test
    void countMayElideMapSinceJava9() {
        // The JDK may skip map() when it can compute the size directly: side effects in map are unreliable.
        int seen = StreamPitfalls.sideEffectsInMapBad(List.of(1, 2, 3));
        assertTrue(seen == 0 || seen == 3);
    }

    @Test
    void snapshotAndPlainLoop() {
        assertEquals(List.of(1, 2), StreamPitfalls.snapshotThenStream(new java.util.ArrayList<>(List.of(1, -1, 2))));
        assertEquals(1, StreamPitfalls.indexOf(List.of("a", "b"), "b").orElseThrow());
        assertTrue(StreamPitfalls.indexOf(List.of(), "b").isEmpty());
    }
}
