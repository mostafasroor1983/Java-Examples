package module06.collectors;

import module06.basics.Fixtures;
import module06.basics.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectorDemosTest {
    private final List<Transaction> txs = Fixtures.sample();

    @Test
    void groupingAndPartitioning() {
        Map<Transaction.Category, BigDecimal> spend = CollectorDemos.spendByCategory(txs);
        assertEquals(new BigDecimal("45.50"), spend.get(Transaction.Category.GROCERIES));
        assertEquals(new BigDecimal("500.00"), spend.get(Transaction.Category.FX));
        assertNull(spend.get(Transaction.Category.SALARY));
        var parts = CollectorDemos.debitsVsCredits(txs);
        assertEquals(5, parts.get(true).size());
        assertEquals(2, parts.get(false).size());
    }

    @Test
    void toMapDuplicateKeysThrowWithoutMerge() {
        assertThrows(IllegalStateException.class, () -> CollectorDemos.amountByAccountBad(txs));
        Map<String, BigDecimal> ok = CollectorDemos.amountByAccount(txs);
        assertEquals(new BigDecimal("1442.50"), ok.get("A"));
        assertEquals(new BigDecimal("-301.99"), ok.get("B"));
    }

    @Test
    void joiningTeeingCountingNested() {
        assertEquals("[t1,t2]", CollectorDemos.csvIds(txs.subList(0, 2)));
        assertEquals(new BigDecimal("422.78"), CollectorDemos.averageAbsAmount(txs).orElseThrow());
        assertTrue(CollectorDemos.averageAbsAmount(List.of()).isEmpty());
        assertEquals(4L, CollectorDemos.countByAccount(txs).get("A"));
        assertEquals(1L, CollectorDemos.countByAccountAndCategory(txs).get("B").get(Transaction.Category.FEES));
    }

    @Test
    void customBigDecimalCollectorWorksSequentialAndParallel() {
        BigDecimal seq = txs.stream().collect(CollectorDemos.summingBigDecimal(Transaction::amount));
        BigDecimal par = txs.parallelStream().collect(CollectorDemos.summingBigDecimal(Transaction::amount));
        assertEquals(new BigDecimal("1240.51"), seq);
        assertEquals(seq, par);
    }
}
