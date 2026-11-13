package module06.exercise1;

import module06.basics.Fixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskReportTest {

    @Test
    void buildsPerAccountRiskSortedByVolume() {
        var report = RiskReport.build(Fixtures.sample(), new BigDecimal("400"));
        assertEquals(2, report.size());
        var a = report.get(0);
        assertEquals("A", a.accountId());
        assertEquals(3, a.debitCount());
        assertEquals(new BigDecimal("500.00"), a.largestDebit());
        assertEquals(new BigDecimal("557.50"), a.debitVolume());
        assertTrue(a.flagged());
        var b = report.get(1);
        assertEquals(new BigDecimal("301.99"), b.debitVolume());
        assertFalse(b.flagged());
        assertEquals(List.of("A"), RiskReport.flaggedAccounts(Fixtures.sample(), new BigDecimal("400")));
    }

    @Test
    void emptyInput() {
        assertTrue(RiskReport.build(List.of(), BigDecimal.ONE).isEmpty());
    }
}
