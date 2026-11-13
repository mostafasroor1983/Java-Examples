package module08.resources;

import java.util.ArrayList;
import java.util.List;

/** A fake resource that records lifecycle events so tests can prove ordering of close() calls. */
public final class LedgerConnection implements AutoCloseable {
    private final String name;
    private final List<String> events;
    private final boolean failOnClose;
    private boolean open = true;

    public LedgerConnection(String name, List<String> events, boolean failOnClose) {
        this.name = name;
        this.events = events;
        this.failOnClose = failOnClose;
        events.add("open " + name);
    }

    public void write(String entry) {
        if (!open) throw new IllegalStateException("closed");
        if (entry.contains("poison")) throw new IllegalArgumentException("bad entry: " + entry);
        events.add(name + " write " + entry);
    }

    @Override
    public void close() {
        open = false;
        events.add("close " + name);
        if (failOnClose) throw new IllegalStateException("close failed: " + name);
    }

    public static List<String> newLog() {
        return new ArrayList<>();
    }
}
