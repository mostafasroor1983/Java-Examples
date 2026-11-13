package module15.p15history;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class P15Test {
    private static Instant t(long s) {
        return Instant.ofEpochSecond(s);
    }

    @Test
    void rangeQueriesBalanceAsOfAndPagination() {
        var h = new P15TransactionHistory();
        h.append(t(10), "A", new BigDecimal("100"), "salary");
        h.append(t(20), "A", new BigDecimal("-30"), "groceries");
        h.append(t(20), "B", new BigDecimal("5"), "other account");
        h.append(t(30), "A", new BigDecimal("-20"), "transport");
        var mid = h.append(t(40), "A", new BigDecimal("-5"), "coffee");

        assertEquals(2, h.between("A", t(20), t(40)).size());
        assertEquals(new BigDecimal("70"), h.balanceAsOf("A", t(20)));
        assertEquals(new BigDecimal("45"), h.balanceAsOf("A", t(100)));
        assertEquals(BigDecimal.ZERO, h.balanceAsOf("A", t(5)));

        var page1 = h.latest("A", 2, null);
        assertEquals(List.of("coffee", "transport"), page1.stream().map(P15TransactionHistory.Tx::description).toList());
        var page2 = h.latest("A", 2, page1.get(1));
        assertEquals(List.of("groceries", "salary"), page2.stream().map(P15TransactionHistory.Tx::description).toList());
        assertTrue(h.latest("A", 2, page2.get(1)).isEmpty());
        assertEquals(mid, h.latest("A", 1, null).get(0));
        assertEquals(5, h.size());
        assertThrows(IllegalArgumentException.class, () -> h.append(t(1), "A", BigDecimal.ZERO, ""));
        assertThrows(UnsupportedOperationException.class, () -> h.between("A", t(0), t(100)).clear());
    }
}
