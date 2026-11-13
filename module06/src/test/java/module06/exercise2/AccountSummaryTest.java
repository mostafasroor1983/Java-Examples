package module06.exercise2;

import module06.basics.Fixtures;
import module06.basics.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountSummaryTest {

    @Test
    void monthlySummaryForAccountA() {
        var s = AccountSummary.summarise("A", YearMonth.of(2024, 3), new BigDecimal("100.00"), Fixtures.sample());
        assertEquals(new BigDecimal("2000.00"), s.credits());
        assertEquals(new BigDecimal("557.50"), s.debits());
        assertEquals(new BigDecimal("1542.50"), s.closing());
        assertEquals(new BigDecimal("45.50"), s.spendByCategory().get(Transaction.Category.GROCERIES));
        assertEquals(LocalDate.parse("2024-03-02"), s.busiestDay().orElseThrow());
    }

    @Test
    void otherMonthHasNoActivity() {
        var s = AccountSummary.summarise("A", YearMonth.of(2024, 4), BigDecimal.TEN, Fixtures.sample());
        assertEquals(BigDecimal.TEN, s.closing());
        assertTrue(s.busiestDay().isEmpty());
        assertTrue(s.spendByCategory().isEmpty());
        assertEquals(BigDecimal.ZERO, AccountSummary.summarise("Z", YearMonth.of(2024, 3), BigDecimal.ZERO, List.of()).credits());
    }
}
