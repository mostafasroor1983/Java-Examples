package module01.solid.dip.bad;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Low-level detail the BAD TransferService depends on directly (simulated SQL rows, no real database). */
public class MySqlLedger {
    private final List<String> rows = new ArrayList<>();

    public void insert(String from, String to, BigDecimal amount) {
        rows.add("INSERT INTO ledger VALUES ('" + from + "','" + to + "'," + amount + ")");
    }

    public List<String> rows() {
        return rows;
    }
}
