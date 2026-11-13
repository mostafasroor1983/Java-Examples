package module06.basics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StreamBasicsTest {
    private final List<Transaction> txs = Fixtures.sample();

    @Test
    void filterSortLimit() {
        var big = StreamBasics.debitsOver(txs, new BigDecimal("40"));
        assertEquals(List.of("t6", "t4", "t2"), big.stream().map(Transaction::id).toList());
        assertEquals(Set.of("EUR", "GBP", "USD"), StreamBasics.currenciesUsed(txs));
    }

    @Test
    void reduceAndMax() {
        assertEquals(new BigDecimal("1240.51"), StreamBasics.net(txs));
        assertEquals("t1", StreamBasics.largestCredit(txs).orElseThrow().id());
        assertTrue(StreamBasics.largestCredit(List.of()).isEmpty());
    }

    @Test
    void flatMapAnyMatchPrimitive() {
        assertEquals(7, StreamBasics.allIds(List.of(txs.subList(0, 3), txs.subList(3, 7))).size());
        assertTrue(StreamBasics.anyFxToday(txs, LocalDate.parse("2024-03-05")));
        assertFalse(StreamBasics.anyFxToday(txs, LocalDate.parse("2024-03-06")));
        assertEquals(55, StreamBasics.sumOfSquares(5));
        assertEquals(List.of("A", "B"), StreamBasics.topNAccountsByVolume(txs, 2));
    }
}
