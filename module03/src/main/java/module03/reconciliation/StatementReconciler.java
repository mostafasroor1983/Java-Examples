package module03.reconciliation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * End-of-day reconciliation: compare what OUR ledger says happened with what the bank / scheme statement says.
 *
 * <p>Algorithm: index the statement lines by reference in a {@link HashMap} (O(n)), then walk the ledger once
 * (O(m)). Overall O(n + m) instead of the naive O(n x m) nested loop, which matters for millions of lines per day.
 * Duplicates on either side are reported explicitly because they usually mean double booking or a re-sent file.
 *
 * <p>Results keep ledger order ({@link LinkedHashMap}) so reports are stable for auditors; the per-status summary
 * uses an {@link EnumMap} (array-backed, ordered by enum declaration, faster than HashMap for enum keys).
 */
public final class StatementReconciler {

    public enum Status { MATCHED, AMOUNT_MISMATCH, MISSING_IN_STATEMENT, UNEXPECTED_IN_STATEMENT, DUPLICATE }

    public record Line(String reference, BigDecimal amount, String currency) {
        public Line {
            Objects.requireNonNull(reference, "reference");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(currency, "currency");
        }
    }

    public record Break(String reference, Status status, Line ledger, Line statement) {
    }

    public record Report(Map<String, Break> results, EnumMap<Status, Integer> summary) {
        public boolean fullyReconciled() {
            return results.values().stream().allMatch(b -> b.status() == Status.MATCHED);
        }

        public List<Break> breaks() {
            return results.values().stream().filter(b -> b.status() != Status.MATCHED).toList();
        }
    }

    public Report reconcile(List<Line> ledger, List<Line> statement) {
        Map<String, Line> statementByRef = new HashMap<>(capacityFor(statement.size()));
        Map<String, Break> results = new LinkedHashMap<>();

        for (Line s : statement) {
            if (statementByRef.putIfAbsent(s.reference(), s) != null) {
                results.put("STMT-DUP:" + s.reference(), new Break(s.reference(), Status.DUPLICATE, null, s));
            }
        }
        for (Line l : ledger) {
            if (results.containsKey(l.reference())) {
                results.put("LEDGER-DUP:" + l.reference(), new Break(l.reference(), Status.DUPLICATE, l, null));
                continue;
            }
            Line s = statementByRef.remove(l.reference()); // remove: what is left afterwards is "unexpected"
            Status status;
            if (s == null) status = Status.MISSING_IN_STATEMENT;
            else if (!l.currency().equals(s.currency()) || l.amount().compareTo(s.amount()) != 0) status = Status.AMOUNT_MISMATCH;
            else status = Status.MATCHED; // compareTo, not equals: 10.0 and 10.00 are the same amount
            results.put(l.reference(), new Break(l.reference(), status, l, s));
        }
        statementByRef.values().forEach(s ->
                results.put(s.reference(), new Break(s.reference(), Status.UNEXPECTED_IN_STATEMENT, null, s)));

        EnumMap<Status, Integer> summary = new EnumMap<>(Status.class);
        for (Status st : Status.values()) summary.put(st, 0);
        results.values().forEach(b -> summary.merge(b.status(), 1, Integer::sum));
        return new Report(Collections.unmodifiableMap(results), summary);
    }

    /** Pre-size so a known number of entries never triggers a resize (see HashMapInternals.capacityFor). */
    private static int capacityFor(int expected) {
        return (int) Math.ceil(expected / 0.75) + 1;
    }

    /** Convenience for reports. */
    public static List<String> describe(Report r) {
        List<String> out = new ArrayList<>();
        r.breaks().forEach(b -> out.add(b.status() + " " + b.reference()));
        return out;
    }
}
