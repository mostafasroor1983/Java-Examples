package module03.history;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BalanceHistoryTest {
    private static final LocalDate D1 = LocalDate.of(2024, 3, 1);

    @Test
    void daysWithoutActivityInheritThePreviousBalance() {
        BalanceHistory h = new BalanceHistory();
        h.post(D1, new BigDecimal("100"));
        h.post(D1.plusDays(5), new BigDecimal("-30"));
        assertEquals(BigDecimal.ZERO, h.balanceAsOf(D1.minusDays(1)));
        assertEquals(new BigDecimal("100"), h.balanceAsOf(D1.plusDays(4)));
        assertEquals(new BigDecimal("70"), h.balanceAsOf(D1.plusDays(30)));
        assertEquals(2, h.storedDays(), "sparse storage: one row per active day");
    }

    @Test
    void backValuedPostingShiftsAllLaterBalances() {
        BalanceHistory h = new BalanceHistory();
        h.post(D1, new BigDecimal("100"));
        h.post(D1.plusDays(10), new BigDecimal("50"));
        h.post(D1.plusDays(3), new BigDecimal("-20")); // correction with an earlier value date
        assertEquals(new BigDecimal("100"), h.balanceAsOf(D1.plusDays(2)));
        assertEquals(new BigDecimal("80"), h.balanceAsOf(D1.plusDays(3)));
        assertEquals(new BigDecimal("130"), h.balanceAsOf(D1.plusDays(10)));
    }

    @Test
    void minimumAndBalanceDays() {
        BalanceHistory h = new BalanceHistory();
        h.post(D1, new BigDecimal("100"));
        h.post(D1.plusDays(2), new BigDecimal("-90"));
        h.post(D1.plusDays(3), new BigDecimal("40"));
        assertEquals(new BigDecimal("10"), h.minimumBalance(D1, D1.plusDays(5)));
        assertEquals(new BigDecimal("100"), h.minimumBalance(D1, D1.plusDays(1)));
        // 100 + 100 + 10 + 50 = 260 over 4 days
        assertEquals(new BigDecimal("260"), h.balanceDays(D1, D1.plusDays(3)));
        assertThrows(IllegalArgumentException.class, () -> h.minimumBalance(D1.plusDays(1), D1));
        assertThrows(IllegalArgumentException.class, () -> h.post(D1, BigDecimal.ZERO));
    }
}
