package module18;

import module18.ch05.*;
import module18.ch05.Item31BoundedWildcards.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.random.RandomGeneratorFactory;

import static org.junit.jupiter.api.Assertions.*;

/** Chapter 5 - Generics (items 26-33). */
class Chapter05Test {

    @Test
    void item26_rawTypeFailsFarFromTheCause() {
        List<String> refs = Item26RawTypes.newRefs();
        Item26RawTypes.addLegacyEntry(refs);
        assertThrows(ClassCastException.class, () -> Item26RawTypes.firstReference(refs));
        assertEquals(2, Item26RawTypes.countDistinct(List.of("a", 1, "a")));
    }

    @Test
    void item27_narrowSuppressionStillTypeSafe() {
        var b = new Item27UncheckedWarnings.Buffer<String>();
        b.add("E1");
        b.add("E2");
        b.add("E3");
        assertEquals("E3", b.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> b.get(3));
    }

    @Test
    void item28_arraysFailAtRuntimeListsAtCompileTime() {
        assertThrows(ArrayStoreException.class, Item28ListsOverArrays::covariantArrayStore);
        var chooser = new Item28ListsOverArrays.Chooser<>(List.of("psp-a", "psp-b"),
                RandomGeneratorFactory.of("L64X128MixRandom").create(42));
        assertTrue(Set.of("psp-a", "psp-b").contains(chooser.choose()));
        assertThrows(IllegalArgumentException.class, () -> new Item28ListsOverArrays.Chooser<>(List.of(), null));
    }

    @Test
    void item29_genericBoundedQueue() {
        var q = new Item29GenericTypes.BoundedQueue<BigDecimal>(2);
        assertTrue(q.offer(BigDecimal.ONE));
        assertTrue(q.offer(BigDecimal.TEN));
        assertFalse(q.offer(BigDecimal.ZERO), "back-pressure");
        assertEquals(BigDecimal.ONE, q.poll().orElseThrow());
    }

    @Test
    void item30_genericMethodsInferTypes() {
        Set<Number> u = Item30GenericMethods.union(Set.of(1, 2), Set.of(2.5));
        assertEquals(3, u.size());
        assertEquals(new BigDecimal("9.99"), Item30GenericMethods.max(List.of(new BigDecimal("1.00"), new BigDecimal("9.99"))));
        assertEquals("x", Item30GenericMethods.<String>identity().apply("x"));
        assertThrows(IllegalArgumentException.class, () -> Item30GenericMethods.max(List.<Integer>of()));
    }

    @Test
    void item31_pecs() {
        var buffer = new Buffer<Payment>();
        buffer.pushAll(List.of(new CardPayment("C1"), new CardPayment("C2"))); // List<CardPayment> accepted
        List<Object> sink = new ArrayList<>();
        buffer.drainTo(sink);                                                  // List<Object> accepted
        assertEquals(2, sink.size());
    }

    @Test
    void item32_safeVarargsVsExposedArray() {
        assertEquals(List.of(1, 2, 3), Item32GenericsAndVarargs.flatten(List.of(1), List.of(2, 3)));
        assertThrows(ClassCastException.class, () -> Item32GenericsAndVarargs.pickTwoStrings("a", "b"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void item33_typesafeHeterogeneousContainer() {
        var ctx = new Item33HeterogeneousContainer.PaymentContext();
        ctx.put(String.class, "trace-42");
        ctx.put(Integer.class, 87);
        assertEquals("trace-42", ctx.get(String.class).orElseThrow());
        assertEquals(87, ctx.get(Integer.class).orElseThrow());
        assertTrue(ctx.get(Long.class).isEmpty());
        Class raw = Integer.class;
        assertThrows(ClassCastException.class, () -> ctx.put(raw, "not an int"), "Class.cast blocks raw-type abuse");
    }
}
