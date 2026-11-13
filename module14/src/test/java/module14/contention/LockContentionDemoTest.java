package module14.contention;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LockContentionDemoTest {

    @Test
    void allThreeStoresAreCorrectUnderContention() throws Exception {
        int threads = 8, per = 10_000;
        for (LockContentionDemo.BalanceStore store : new LockContentionDemo.BalanceStore[]{
                new LockContentionDemo.CoarseLocked(), new LockContentionDemo.Striped(16), new LockContentionDemo.Chm()}) {
            long nanos = LockContentionDemo.hammer(store, threads, per);
            assertTrue(nanos > 0);
            for (int t = 0; t < threads; t++) {
                assertEquals(new BigDecimal(per), store.balance("acc-" + t), store.getClass().getSimpleName());
            }
            assertEquals(BigDecimal.ZERO, store.balance("nobody"));
        }
    }

    @Test
    void stripingMapsSameAccountToSameStripe() {
        var s = new LockContentionDemo.Striped(4);
        s.credit("acc-1", BigDecimal.ONE);
        s.credit("acc-1", BigDecimal.ONE);
        assertEquals(new BigDecimal("2"), s.balance("acc-1"));
    }
}
