package module04.cow;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class FraudRuleRegistryTest {

    @Test
    void evaluateReturnsMatchingRuleNames() {
        FraudRuleRegistry r = new FraudRuleRegistry();
        r.register(new FraudRuleRegistry.Rule("big", a -> a.compareTo(new BigDecimal("1000")) > 0));
        r.register(new FraudRuleRegistry.Rule("huge", a -> a.compareTo(new BigDecimal("10000")) > 0));
        assertEquals(java.util.List.of("big"), r.evaluate(new BigDecimal("5000")));
        assertTrue(r.evaluate(BigDecimal.ONE).isEmpty());
        assertTrue(r.unregister("big"));
        assertFalse(r.unregister("big"));
        assertEquals(1, r.size());
    }

    @Test
    void concurrentRegistrationDuringIterationDoesNotThrow() throws Exception {
        FraudRuleRegistry r = new FraudRuleRegistry();
        for (int i = 0; i < 50; i++) r.register(new FraudRuleRegistry.Rule("r" + i, a -> true));
        AtomicBoolean failed = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(2);
        Thread reader = new Thread(() -> {
            try {
                for (int i = 0; i < 2000; i++) r.evaluate(BigDecimal.ONE);
            } catch (Throwable t) {
                failed.set(true);
            } finally {
                done.countDown();
            }
        });
        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < 500; i++) r.register(new FraudRuleRegistry.Rule("w" + i, a -> false));
            } catch (Throwable t) {
                failed.set(true);
            } finally {
                done.countDown();
            }
        });
        reader.start();
        writer.start();
        done.await();
        assertFalse(failed.get());
        assertEquals(550, r.size());
    }
}
