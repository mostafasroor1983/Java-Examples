package module01.solid.dip.good;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryLedger implements Ledger {
    public record Entry(String from, String to, BigDecimal amount) {
    }

    private final List<Entry> entries = new ArrayList<>();

    @Override
    public void record(String from, String to, BigDecimal amount) {
        entries.add(new Entry(from, to, amount));
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }
}
