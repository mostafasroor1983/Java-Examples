package module18.ch02;

import java.util.ArrayList;
import java.util.List;

/**
 * Item 9 - Prefer try-with-resources to try-finally.
 *
 * <p>Nested try-finally blocks are verbose and, worse, an exception from {@code close()} in {@code finally}
 * REPLACES the original exception, hiding the root cause of a failed settlement upload. try-with-resources closes in
 * reverse order and attaches close() failures as SUPPRESSED exceptions to the primary one.
 */
public final class Item09TryWithResources {
    private Item09TryWithResources() {
    }

    public static final class Resource implements AutoCloseable {
        private final String name;
        private final boolean failOnClose;
        private final List<String> log;

        public Resource(String name, boolean failOnClose, List<String> log) {
            this.name = name;
            this.failOnClose = failOnClose;
            this.log = log;
            log.add("open " + name);
        }

        @Override
        public void close() {
            log.add("close " + name);
            if (failOnClose) throw new IllegalStateException("close failed: " + name);
        }
    }

    /** BAD: the close failure masks the real upload failure. */
    public static void uploadTryFinally(List<String> log) {
        Resource conn = new Resource("sftp", true, log);
        try {
            throw new IllegalStateException("upload failed");
        } finally {
            conn.close();
        }
    }

    /** GOOD: primary exception kept, close failures suppressed, closed in reverse order. */
    public static void uploadTryWithResources(List<String> log) {
        try (Resource conn = new Resource("sftp", true, log);
             Resource file = new Resource("file", true, log)) {
            throw new IllegalStateException("upload failed");
        }
    }

    public static List<String> newLog() {
        return new ArrayList<>();
    }
}
