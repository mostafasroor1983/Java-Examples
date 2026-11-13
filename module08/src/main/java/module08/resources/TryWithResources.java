package module08.resources;

import java.util.List;

/** try-with-resources: resources close in reverse order, close() exceptions are suppressed, not lost. */
public final class TryWithResources {
    private TryWithResources() {
    }

    /** BAD: pre-Java-7 pattern. If write() throws, close() never runs -> leaked connection. */
    public static void writeBad(List<String> log, String entry) {
        LedgerConnection c = new LedgerConnection("primary", log, false);
        c.write(entry);
        c.close();
    }

    /** GOOD: close always runs, even when write throws. */
    public static void writeGood(List<String> log, String entry) {
        try (LedgerConnection c = new LedgerConnection("primary", log, false)) {
            c.write(entry);
        }
    }

    /** Multiple resources close in reverse declaration order. */
    public static void writeToBoth(List<String> log, String entry) {
        try (LedgerConnection primary = new LedgerConnection("primary", log, false);
             LedgerConnection replica = new LedgerConnection("replica", log, false)) {
            primary.write(entry);
            replica.write(entry);
        }
    }

    /** When both body and close() throw, the body exception wins and close()'s is suppressed. */
    public static void writeWithFailingClose(List<String> log, String entry) {
        try (LedgerConnection c = new LedgerConnection("flaky", log, true)) {
            c.write(entry);
        }
    }

    /** Java 9+: effectively-final existing variable can be used directly. */
    public static void writeExisting(LedgerConnection existing, String entry) {
        try (existing) {
            existing.write(entry);
        }
    }

    /** finally still has its place: for non-resource cleanup (metrics, MDC). */
    public static long timed(Runnable body, List<String> log) {
        long start = System.nanoTime();
        try {
            body.run();
            return System.nanoTime() - start;
        } finally {
            log.add("timer stopped");
        }
    }
}
