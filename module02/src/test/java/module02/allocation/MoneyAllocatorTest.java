package module02.allocation;

import module02.valueobjects.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MoneyAllocatorTest {

    private static Money sum(List<Money> parts) {
        return parts.stream().reduce(Money::add).orElseThrow();
    }

    @Test
    void evenSplitNeverLosesACent() {
        List<Money> parts = MoneyAllocator.splitEvenly(Money.of("100.00", "EUR"), 3);
        assertEquals(List.of(Money.of("33.34", "EUR"), Money.of("33.33", "EUR"), Money.of("33.33", "EUR")), parts);
        assertEquals(Money.of("100.00", "EUR"), sum(parts));
    }

    @Test
    void ratiosAndZeroDecimalCurrencies() {
        assertEquals(List.of(Money.of("70", "JPY"), Money.of("30", "JPY")),
                MoneyAllocator.allocate(Money.of("100", "JPY"), List.of(7, 3)));
        assertEquals(List.of(Money.of("0.00", "EUR"), Money.of("0.01", "EUR")),
                MoneyAllocator.allocate(Money.of("0.01", "EUR"), List.of(0, 1)), "zero ratio gets nothing");
    }

    @Test
    void negativeAmountsForRefundSplits() {
        List<Money> parts = MoneyAllocator.splitEvenly(Money.of("-10.00", "EUR"), 3);
        assertEquals(Money.of("-10.00", "EUR"), sum(parts));
    }

    @Test
    void propertyTotalIsAlwaysPreserved() {
        Random rnd = new Random(42); // fixed seed: reproducible property-style test without extra libraries
        for (int run = 0; run < 2_000; run++) {
            Money total = Money.of(BigDecimal.valueOf(rnd.nextInt(2_000_000) - 1_000_000, 2), java.util.Currency.getInstance("GBP"));
            List<Integer> ratios = rnd.ints(1 + rnd.nextInt(7), 0, 10).boxed().toList();
            if (ratios.stream().mapToInt(Integer::intValue).sum() == 0) continue;
            List<Money> parts = MoneyAllocator.allocate(total, ratios);
            assertEquals(total, sum(parts), () -> "lost money for " + total + " " + ratios);
            for (int i = 0; i < ratios.size(); i++) if (ratios.get(i) == 0) assertEquals(0, parts.get(i).amount().signum());
        }
    }

    @Test
    void invalidInputs() {
        Money m = Money.of("1.00", "EUR");
        assertThrows(IllegalArgumentException.class, () -> MoneyAllocator.allocate(m, List.of()));
        assertThrows(IllegalArgumentException.class, () -> MoneyAllocator.allocate(m, List.of(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> MoneyAllocator.allocate(m, List.of(1, -1)));
        assertThrows(IllegalArgumentException.class, () -> MoneyAllocator.splitEvenly(m, 0));
    }
}
