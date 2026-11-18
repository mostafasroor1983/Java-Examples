package module18;

import module18.ch09.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

/** Chapter 9 - General programming (items 57-68). */
class Chapter09Test {

    @Test
    void item57_scopeBugSilentlySkipsWork() {
        assertEquals(2, Item57LocalVariableScope.copyPasteBug(List.of("a", "b"), List.of("c", "d")));
        assertEquals(4, Item57LocalVariableScope.processAll(List.of("a", "b"), List.of("c", "d")));
    }

    @Test
    void item58_nestedIteratorBug() {
        assertEquals(6, Item58ForEach.pairs(List.of("c1", "c2", "c3"), List.of("m1", "m2")).size());
        assertThrows(NoSuchElementException.class, () -> Item58ForEach.pairsBuggy(List.of("c1", "c2", "c3"), List.of("m1", "m2")));
    }

    @Test
    void item59_libraryBeatsHandRolled() {
        assertTrue(Item59KnowTheLibraries.shardBad(Integer.MIN_VALUE, 3) < 0, "Math.abs(MIN_VALUE) is negative");
        assertTrue(Item59KnowTheLibraries.shard(Integer.MIN_VALUE, 3) >= 0);
        int p = Item59KnowTheLibraries.pickProvider(RandomGenerator.of("L64X128MixRandom"), 4);
        assertTrue(p >= 0 && p < 4);
        assertEquals("0aff", Item59KnowTheLibraries.hex(new byte[]{10, (byte) 255}));
    }

    @Test
    void item60_doubleGivesWrongMoneyAnswers() {
        assertEquals(0.6100000000000001, 1.03 - 0.42);
        assertEquals(3, Item60AvoidFloatDouble.itemsBoughtWithDouble());
        assertEquals(4, Item60AvoidFloatDouble.itemsBoughtWithBigDecimal());
        assertEquals(4, Item60AvoidFloatDouble.itemsBoughtWithCents());
    }

    @Test
    void item61_boxedPitfalls() {
        assertEquals(1, Item61PrimitivesOverBoxed.BROKEN.compare(1000, 1000), "== on Integer outside the cache");
        assertEquals(0, Item61PrimitivesOverBoxed.FIXED.compare(1000, 1000));
        assertThrows(NullPointerException.class, Item61PrimitivesOverBoxed::isHighRisk);
    }

    @Test
    void item62_typesInsteadOfCompoundStrings() {
        assertEquals(new BigDecimal("100.00"), Item62AvoidStrings.amountFromCompound("EUR:100.00"));
        var a = Item62AvoidStrings.Amount.parse("EUR:100.00");
        assertEquals("EUR", a.currency().getCurrencyCode());
        assertThrows(IllegalArgumentException.class, () -> Item62AvoidStrings.Amount.parse("EURO:1"));
    }

    @Test
    void item63_builderSameOutput() {
        var lines = Collections.nCopies(500, "2024-03-01 Card payment -12.30");
        assertEquals(Item63StringConcatenation.renderSlow(lines), Item63StringConcatenation.render(lines));
    }

    @Test
    void item64_sequencedCollectionInterface() {
        assertEquals("T1..T2", Item64ReferToInterfaces.firstAndLast(Item64ReferToInterfaces.newHistory()));
        assertEquals("T1..T2", Item64ReferToInterfaces.firstAndLast(Item64ReferToInterfaces.uniqueHistory()));
        assertEquals("x..z", Item64ReferToInterfaces.firstAndLast(new ArrayDeque<>(List.of("x", "y", "z"))));
    }

    @Test
    void item65_reflectiveInstantiationInterfaceUse() {
        var policy = Item65InterfacesOverReflection.load(Item65InterfacesOverReflection.FlatOneEuro.class.getName());
        assertEquals(BigDecimal.ONE, policy.fee(BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> Item65InterfacesOverReflection.load("no.such.Policy"));
        assertThrows(IllegalArgumentException.class, () -> Item65InterfacesOverReflection.load("java.lang.String"));
    }

    @Test
    void item66_pureJavaChecksum() {
        assertEquals(Item66NativeMethods.fileChecksum("batch-1"), Item66NativeMethods.fileChecksum("batch-1"));
        assertNotEquals(Item66NativeMethods.fileChecksum("batch-1"), Item66NativeMethods.fileChecksum("batch-2"));
    }

    @Test
    void item67_measureDontGuess() {
        var m = Item67OptimizeJudiciously.measure(() -> Item67OptimizeJudiciously.total(List.of(BigDecimal.ONE, BigDecimal.TEN)));
        assertEquals(new BigDecimal("11"), m.result());
        assertTrue(m.nanos() >= 0);
    }

    @Test
    void item68_conventionalNames() {
        var acc = Item68NamingConventions.AccountNumber.from("DE89370400440532013000");
        assertEquals("0532013000", acc.value());
        assertFalse(acc.isDomestic());
        assertEquals("******3000", acc.toMaskedString());
        assertEquals(1250, Item68NamingConventions.toMinorUnits(new BigDecimal("12.50")));
    }
}
