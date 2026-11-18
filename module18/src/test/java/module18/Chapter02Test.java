package module18;

import module18.ch02.*;
import module18.ch02.Item01StaticFactories.Money;
import module18.ch02.Item02Builder.PaymentInstruction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Chapter 2 - Creating and destroying objects (items 1-9). */
class Chapter02Test {

    @Test
    void item01_namedAndCachingFactories() {
        assertEquals(Money.of("12.50", "EUR"), Money.parse(" EUR 12.5 "));
        assertEquals(Money.of("12.50", "EUR"), Money.ofMinor(1250, "EUR"));
        assertSame(Money.zero("GBP"), Money.zero("GBP"), "instance control");
        assertThrows(ArithmeticException.class, () -> Money.of("1.001", "EUR"));
    }

    @Test
    void item02_builderValidatesOnce() {
        PaymentInstruction p = PaymentInstruction.builder("DE01", "FR02", new BigDecimal("10"))
                .reference("INV-1").priority(Item02Builder.Priority.URGENT).build();
        assertEquals(Item02Builder.Priority.URGENT, p.priority());
        assertNull(p.executionDate(), "optional parameter left unset");
        assertThrows(IllegalArgumentException.class,
                () -> PaymentInstruction.builder("DE01", "DE01", BigDecimal.ONE).build());
        assertThrows(IllegalArgumentException.class,
                () -> PaymentInstruction.builder("DE01", "FR02", new BigDecimal("200000")).instant(true).build());
    }

    @Test
    void item03_enumSingletonAndReflectionGuard() throws Exception {
        var gen = Item03Singleton.PaymentReferenceGenerator.INSTANCE;
        assertNotEquals(gen.next(), gen.next());
        assertNotNull(Item03Singleton.LegacyRegistry.INSTANCE);
        var ctor = Item03Singleton.LegacyRegistry.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        var ex = assertThrows(java.lang.reflect.InvocationTargetException.class, ctor::newInstance);
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    @Test
    void item04_utilityClassCannotBeInstantiated() throws Exception {
        assertEquals(new BigDecimal("1.45"), Item04Noninstantiable.applyBps(new BigDecimal("100"), 145));
        var ctor = Item04Noninstantiable.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        var ex = assertThrows(java.lang.reflect.InvocationTargetException.class, ctor::newInstance);
        assertInstanceOf(AssertionError.class, ex.getCause());
    }

    @Test
    void item05_injectedScheduleAndClock() {
        Clock saturday = Clock.fixed(Instant.parse("2024-03-02T10:00:00Z"), ZoneOffset.UTC);
        var service = new Item05DependencyInjection.FeeService((amt, weekend) -> weekend ? BigDecimal.ONE : BigDecimal.ZERO, saturday);
        assertEquals(BigDecimal.ONE, service.fee(BigDecimal.TEN));
    }

    @Test
    void item06_sameResultWithoutWastefulAllocation() {
        assertTrue(Item06UnnecessaryObjects.looksLikeIban("DE89370400440532013000"));
        assertEquals(Item06UnnecessaryObjects.looksLikeIbanSlow("bad"), Item06UnnecessaryObjects.looksLikeIban("bad"));
        long[] cents = {100, 250, 999};
        assertEquals(Item06UnnecessaryObjects.sumBoxed(cents), Item06UnnecessaryObjects.sum(cents));
    }

    @Test
    void item07_popClearsObsoleteReference() throws Exception {
        var stack = new Item07ObsoleteReferences.PendingStack<String>();
        stack.push("PAY-1");
        stack.push("PAY-2");
        assertEquals("PAY-2", stack.pop());
        var m = stack.getClass().getDeclaredMethod("slotAboveTopIsCleared");
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(stack));
    }

    @Test
    void item08_closeIsTheRealCleanupCleanerOnlyASafetyNet() {
        int before = Item08Cleaners.LEAKS_DETECTED.get();
        try (var export = new Item08Cleaners.StatementExport()) {
            assertTrue(export.isOpen());
        }
        assertEquals(before, Item08Cleaners.LEAKS_DETECTED.get(), "properly closed: no leak reported");
    }

    @Test
    void item09_tryWithResourcesKeepsPrimaryException() {
        List<String> log1 = Item09TryWithResources.newLog();
        var masked = assertThrows(IllegalStateException.class, () -> Item09TryWithResources.uploadTryFinally(log1));
        assertEquals("close failed: sftp", masked.getMessage(), "try-finally lost the real cause");

        List<String> log2 = Item09TryWithResources.newLog();
        var primary = assertThrows(IllegalStateException.class, () -> Item09TryWithResources.uploadTryWithResources(log2));
        assertEquals("upload failed", primary.getMessage());
        assertEquals(2, primary.getSuppressed().length);
        assertEquals(List.of("open sftp", "open file", "close file", "close sftp"), log2, "reverse close order");
    }
}
