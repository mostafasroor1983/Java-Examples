package module15.p02balance;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class P02Test {
    @Test
    void overdraftLimitAndStatement() {
        P02AccountBalance acc = new P02AccountBalance(new BigDecimal("50"));
        acc.deposit(new BigDecimal("100"));
        assertTrue(acc.withdraw(new BigDecimal("150")));
        assertEquals(new BigDecimal("-50"), acc.balance());
        assertFalse(acc.withdraw(new BigDecimal("0.01")));
        assertEquals(2, acc.statement().size());
        assertEquals(new BigDecimal("-50"), acc.statement().get(1).balanceAfter());
        assertThrows(UnsupportedOperationException.class, () -> acc.statement().clear());
        assertThrows(IllegalArgumentException.class, () -> acc.deposit(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new P02AccountBalance(new BigDecimal("-1")));
    }
}
