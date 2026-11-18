package module18;

import module18.ch08.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Chapter 8 - Methods (items 49-56). */
class Chapter08Test {

    @Test
    void item49_failFastWithTheRightException() {
        assertThrows(NullPointerException.class, () -> new Item49ParameterValidity.Transfer(null, "B", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new Item49ParameterValidity.Transfer("A", "B", BigDecimal.ZERO));
        assertThrows(IndexOutOfBoundsException.class, () -> Item49ParameterValidity.nth(List.of(), 0));
    }

    @Test
    void item50_defensiveCopiesBlockTampering() {
        Date start = new Date(0), end = new Date(1_000);
        var leaky = new Item50DefensiveCopies.LeakyPeriod(start, end);
        end.setTime(-1); // attack after validation
        assertTrue(leaky.start().after(leaky.end()), "invariant broken");

        Date s2 = new Date(0), e2 = new Date(1_000);
        var safe = new Item50DefensiveCopies.StatementPeriod(s2, e2);
        e2.setTime(-1);
        safe.end().setTime(-1);
        assertFalse(safe.start().after(safe.end()));
        assertThrows(IllegalArgumentException.class, () -> new Item50DefensiveCopies.Period(Instant.ofEpochSecond(2), Instant.ofEpochSecond(1)));
    }

    @Test
    void item51_parameterObjectAndEnumsReadClearly() {
        String bad = Item51MethodSignatures.scheduleBad("A", "B", "R", BigDecimal.ONE, true, false);
        assertTrue(bad.endsWith("true:false"), "what does true/false mean?");
        var s = new Item51MethodSignatures.PaymentSchedule("A", "B", BigDecimal.ONE, LocalDate.of(2024, 3, 1),
                Item51MethodSignatures.Priority.URGENT, Item51MethodSignatures.Notification.NONE);
        assertEquals("A->B 1 on 2024-03-01 [URGENT, NONE]", Item51MethodSignatures.schedule(s));
    }

    @Test
    void item52_overloadsChosenAtCompileTime() {
        Collection<String> c = Set.of("x");
        assertEquals("collection", Item52Overloading.classify(c), "static type wins");
        assertEquals("set", Item52Overloading.classifyAtRuntime(c));
        List<Integer> ids = new ArrayList<>(List.of(10, 1, 20));
        ids.remove(1); // remove(int index)!
        assertEquals(List.of(10, 20), ids);
        ids = new ArrayList<>(List.of(10, 1, 20));
        ids.remove(Integer.valueOf(1)); // remove(Object)
        assertEquals(List.of(10, 20), ids);
    }

    @Test
    void item53_requiredFirstArgument() {
        assertThrows(IllegalArgumentException.class, Item53Varargs::maxBad);
        assertEquals(BigDecimal.TEN, Item53Varargs.max(BigDecimal.ONE, BigDecimal.TEN));
        assertEquals(BigDecimal.ONE, Item53Varargs.max(BigDecimal.ONE));
    }

    @Test
    void item54_emptyNotNull() {
        assertNull(Item54EmptyNotNull.historyOrNull("ACC-9"));
        assertTrue(Item54EmptyNotNull.history("ACC-9").isEmpty());
        assertEquals(0, Item54EmptyNotNull.tags("ACC-1").length);
    }

    @Test
    void item55_optionalAsReturnType() {
        assertEquals(new BigDecimal("500"), Item55Optionals.effectiveLimit("ACC-1"));
        assertEquals(BigDecimal.ZERO, Item55Optionals.effectiveLimit("ACC-2"));
        assertTrue(Item55Optionals.largestCents(List.of()).isEmpty());
        assertEquals(300, Item55Optionals.largestCents(List.of(100L, 300L)).getAsLong());
    }

    @Test
    void item56_documentedContractHolds() {
        assertEquals(new BigDecimal("109.45"), Item56DocComments.convert(new BigDecimal("100.00"), new BigDecimal("1.1000"), 50));
        assertThrows(IllegalArgumentException.class, () -> Item56DocComments.convert(BigDecimal.ONE, BigDecimal.ONE, 5000));
    }
}
