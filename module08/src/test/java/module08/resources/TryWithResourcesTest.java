package module08.resources;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TryWithResourcesTest {

    @Test
    void badPatternLeaksOnFailure() {
        List<String> log = LedgerConnection.newLog();
        assertThrows(IllegalArgumentException.class, () -> TryWithResources.writeBad(log, "poison"));
        assertFalse(log.contains("close primary"), "connection leaked");
    }

    @Test
    void goodPatternAlwaysCloses() {
        List<String> log = LedgerConnection.newLog();
        assertThrows(IllegalArgumentException.class, () -> TryWithResources.writeGood(log, "poison"));
        assertEquals(List.of("open primary", "close primary"), log);
    }

    @Test
    void multipleResourcesCloseInReverseOrder() {
        List<String> log = LedgerConnection.newLog();
        TryWithResources.writeToBoth(log, "e1");
        assertEquals(List.of("open primary", "open replica", "primary write e1", "replica write e1", "close replica", "close primary"), log);
    }

    @Test
    void closeFailureIsSuppressedBehindBodyFailure() {
        List<String> log = LedgerConnection.newLog();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> TryWithResources.writeWithFailingClose(log, "poison"));
        assertEquals(1, e.getSuppressed().length);
        assertInstanceOf(IllegalStateException.class, e.getSuppressed()[0]);
        // close failure alone propagates on its own
        assertThrows(IllegalStateException.class, () -> TryWithResources.writeWithFailingClose(LedgerConnection.newLog(), "fine"));
    }

    @Test
    void existingVariableAndFinally() {
        List<String> log = LedgerConnection.newLog();
        LedgerConnection c = new LedgerConnection("ext", log, false);
        TryWithResources.writeExisting(c, "e");
        assertTrue(log.contains("close ext"));
        assertThrows(IllegalStateException.class, () -> c.write("after close"));
        assertThrows(RuntimeException.class, () -> TryWithResources.timed(() -> { throw new RuntimeException(); }, log));
        assertTrue(log.contains("timer stopped"));
        assertTrue(TryWithResources.timed(() -> { }, log) >= 0);
    }
}
