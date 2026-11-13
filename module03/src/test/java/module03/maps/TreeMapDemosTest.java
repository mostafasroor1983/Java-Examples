package module03.maps;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TreeMapDemosTest {

    @Test
    void feeTiersUseFloorEntry() {
        var tiers = new TreeMapDemos.FeeTiers().tier("0", "0.02").tier("1000", "0.01").tier("10000", "0.005");
        assertEquals(new BigDecimal("0.02"), tiers.rateFor(new BigDecimal("999.99")));
        assertEquals(new BigDecimal("0.01"), tiers.rateFor(new BigDecimal("1000")));
        assertEquals(new BigDecimal("0.005"), tiers.rateFor(new BigDecimal("50000")));
        assertThrows(IllegalArgumentException.class, () -> tiers.rateFor(new BigDecimal("-1")));
    }

    @Test
    void balanceHistoryRangeAndAsOf() {
        var h = new TreeMapDemos.BalanceHistory();
        h.record(LocalDate.of(2024, 1, 1), new BigDecimal("100"));
        h.record(LocalDate.of(2024, 1, 5), new BigDecimal("150"));
        h.record(LocalDate.of(2024, 1, 10), new BigDecimal("90"));
        assertEquals(2, h.between(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 10)).size());
        assertEquals(new BigDecimal("150"), h.asOf(LocalDate.of(2024, 1, 7)));
        assertEquals(BigDecimal.ZERO, h.asOf(LocalDate.of(2023, 12, 31)));
    }
}
