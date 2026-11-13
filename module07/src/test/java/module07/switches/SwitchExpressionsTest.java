package module07.switches;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;

import static org.junit.jupiter.api.Assertions.*;

class SwitchExpressionsTest {

    @Test
    void oldAndNewAgreeForEveryTier() {
        for (var t : SwitchExpressions.Tier.values()) {
            assertEquals(SwitchExpressions.freeAtmWithdrawalsOld(t), SwitchExpressions.freeAtmWithdrawals(t));
        }
        assertEquals(10, SwitchExpressions.freeAtmWithdrawals(SwitchExpressions.Tier.METAL));
    }

    @Test
    void yieldBlocksAndMultiLabels() {
        assertEquals(new BigDecimal("2.0"), SwitchExpressions.fxMarkupPercent(SwitchExpressions.Tier.STANDARD, DayOfWeek.SUNDAY));
        assertEquals(new BigDecimal("1.0"), SwitchExpressions.fxMarkupPercent(SwitchExpressions.Tier.STANDARD, DayOfWeek.MONDAY));
        assertEquals(new BigDecimal("0.5"), SwitchExpressions.fxMarkupPercent(SwitchExpressions.Tier.PLUS, DayOfWeek.MONDAY));
        assertEquals(new BigDecimal("0.0"), SwitchExpressions.fxMarkupPercent(SwitchExpressions.Tier.METAL, DayOfWeek.SATURDAY));
        assertEquals("groceries", SwitchExpressions.mccCategory("5499"));
        assertEquals("other", SwitchExpressions.mccCategory("9999"));
        assertThrows(NullPointerException.class, () -> SwitchExpressions.freeAtmWithdrawals(null));
    }
}
