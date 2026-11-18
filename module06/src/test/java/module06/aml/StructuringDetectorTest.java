package module06.aml;

import module06.aml.StructuringDetector.Deposit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StructuringDetectorTest {
    private static final LocalDate DAY = LocalDate.of(2024, 3, 1);
    private final StructuringDetector detector = new StructuringDetector(new BigDecimal("10000"), new BigDecimal("0.10"), 2);

    private static Deposit cash(String acc, LocalDate d, String amt) {
        return new Deposit(acc, d, new BigDecimal(amt), true);
    }

    @Test
    void flagsSplitCashDepositsJustBelowThreshold() {
        var alerts = detector.scan(List.of(
                cash("ACC-1", DAY, "9500"), cash("ACC-1", DAY, "9900"),          // flagged
                cash("ACC-2", DAY, "9500"), cash("ACC-2", DAY.plusDays(1), "9500"), // different days
                cash("ACC-3", DAY, "10000"), cash("ACC-3", DAY, "12000"),         // reported anyway (>= threshold)
                new Deposit("ACC-4", DAY, new BigDecimal("9500"), false),
                new Deposit("ACC-4", DAY, new BigDecimal("9500"), false)));       // not cash
        assertEquals(1, alerts.size());
        assertEquals("ACC-1", alerts.get(0).key().accountId());
        assertEquals(new BigDecimal("19400"), alerts.get(0).total());
        assertTrue(detector.worstFor("ACC-1", List.of(cash("ACC-1", DAY, "9500"), cash("ACC-1", DAY, "9900"))).isPresent());
        assertTrue(detector.worstFor("ACC-2", List.of()).isEmpty());
    }

    @Test
    void bandBoundariesAreInclusiveBelowExclusiveAbove() {
        assertEquals(1, detector.scan(List.of(cash("A", DAY, "9000"), cash("A", DAY, "9999.99"))).size());
        assertEquals(0, detector.scan(List.of(cash("A", DAY, "8999.99"), cash("A", DAY, "9999.99"))).size());
    }

    @Test
    void invalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> new StructuringDetector(BigDecimal.TEN, BigDecimal.ONE, 2));
        assertThrows(IllegalArgumentException.class, () -> new StructuringDetector(BigDecimal.TEN, new BigDecimal("0.1"), 1));
    }
}
