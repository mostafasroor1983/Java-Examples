package module03.reconciliation;

import module03.reconciliation.StatementReconciler.Line;
import module03.reconciliation.StatementReconciler.Status;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatementReconcilerTest {
    private static Line l(String ref, String amt) {
        return new Line(ref, new BigDecimal(amt), "EUR");
    }

    @Test
    void classifiesEveryKindOfBreak() {
        var report = new StatementReconciler().reconcile(
                List.of(l("A", "10.00"), l("B", "20.00"), l("C", "30.00"), l("A", "10.00")),
                List.of(l("A", "10.0"), l("B", "21.00"), l("X", "5.00"), l("X", "5.00")));
        assertEquals(Status.MATCHED, report.results().get("A").status(), "scale difference is not a break");
        assertEquals(Status.AMOUNT_MISMATCH, report.results().get("B").status());
        assertEquals(Status.MISSING_IN_STATEMENT, report.results().get("C").status());
        assertEquals(Status.UNEXPECTED_IN_STATEMENT, report.results().get("X").status());
        assertEquals(2, report.summary().get(Status.DUPLICATE));
        assertFalse(report.fullyReconciled());
        assertEquals(1, report.summary().get(Status.MATCHED));
        assertThrows(UnsupportedOperationException.class, () -> report.results().clear());
    }

    @Test
    void currencyMismatchIsABreakEvenWithSameNumber() {
        var report = new StatementReconciler().reconcile(
                List.of(new Line("A", BigDecimal.TEN, "EUR")), List.of(new Line("A", BigDecimal.TEN, "USD")));
        assertEquals(Status.AMOUNT_MISMATCH, report.results().get("A").status());
    }

    @Test
    void largeCleanDayReconcilesInLinearTime() {
        List<Line> ledger = new ArrayList<>(), stmt = new ArrayList<>();
        for (int i = 0; i < 200_000; i++) {
            ledger.add(l("R" + i, i + ".00"));
            stmt.add(l("R" + (199_999 - i), (199_999 - i) + ".00")); // different order on purpose
        }
        var report = new StatementReconciler().reconcile(ledger, stmt);
        assertTrue(report.fullyReconciled());
        assertEquals(200_000, report.summary().get(Status.MATCHED));
        assertTrue(StatementReconciler.describe(report).isEmpty());
    }

    @Test
    void emptyInputs() {
        var report = new StatementReconciler().reconcile(List.of(), List.of());
        assertTrue(report.fullyReconciled());
    }
}
