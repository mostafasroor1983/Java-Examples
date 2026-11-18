package module18;

import module18.ch04.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/** Chapter 4 - Classes and interfaces (items 15-25). */
class Chapter04Test {

    @Test
    void item15_publicArrayIsMutableListIsNot() {
        String[] copy = Item15Accessibility.supportedCopy();
        copy[0] = "XXX";
        assertEquals("EUR", Item15Accessibility.supportedCopy()[0]);
        assertThrows(UnsupportedOperationException.class, () -> Item15Accessibility.SUPPORTED.set(0, "XXX"));
        String saved = Item15Accessibility.SUPPORTED_ARRAY[0];
        Item15Accessibility.SUPPORTED_ARRAY[0] = "XXX"; // the bug: anyone can do this
        assertEquals("XXX", Item15Accessibility.SUPPORTED_ARRAY[0]);
        Item15Accessibility.SUPPORTED_ARRAY[0] = saved;
    }

    @Test
    void item16_accessorsEnforceInvariants() {
        var bad = new Item16Accessors.PublicFieldsLimit();
        bad.dailyLimit = new BigDecimal("-1"); // nothing stops this
        assertTrue(bad.dailyLimit.signum() < 0);
        assertThrows(IllegalArgumentException.class, () -> new Item16Accessors.DailyLimit(new BigDecimal("-1"), "EUR"));
    }

    @Test
    void item17_immutableWithWithers() {
        var r = Item17Immutability.InterestRate.ofPercent("3.25");
        var higher = r.plusBasisPoints(25);
        assertEquals(new BigDecimal("3.2500"), r.percent(), "original unchanged");
        assertEquals(new BigDecimal("3.5000"), higher.percent());
        assertSame(Item17Immutability.InterestRate.ZERO, Item17Immutability.InterestRate.ofPercent("0.00"));
        assertEquals(new BigDecimal("0.89"), r.dailyInterest(new BigDecimal("10000")));
    }

    @Test
    void item18_inheritanceDoubleCountsCompositionDoesNot() {
        var bad = new Item18Composition.CountingHashSet<String>();
        bad.addAll(List.of("card-1", "card-2", "card-3"));
        assertEquals(6, bad.addCount(), "self-use of add() inside addAll()");
        var good = new Item18Composition.CountingSet<String>(new TreeSet<>());
        good.addAll(List.of("card-1", "card-2", "card-3"));
        assertEquals(3, good.addCount());
    }

    @Test
    void item19_constructorCallingOverridableMethod() {
        assertEquals("fee: premium discount null", new Item19DesignForInheritance.PremiumFeeCalculator().description());
        assertEquals(new BigDecimal("1.50"),
                new Item19DesignForInheritance.PercentageFee(new BigDecimal("1.5")).feeFor(new BigDecimal("100.00")).setScale(2));
    }

    @Test
    void item20_interfacesWithDefaultsAndSkeleton() {
        var card = new Item20InterfacesOverAbstract.CardChannel();
        assertTrue(card.accepts(new BigDecimal("50")));
        assertFalse(card.accepts(new BigDecimal("10001")));
        assertTrue(card.supportsPartialRefund());
        assertTrue(new Item20InterfacesOverAbstract.SepaInstantChannel().accepts(new BigDecimal("99999")));
    }

    @Test
    void item21_defaultMethodBreaksAtomicityUnlessOverridden() {
        var strict = new Item21InterfacesForPosterity.StrictJournal();
        assertThrows(IllegalArgumentException.class, () -> strict.postAll(List.of("a", " ", "c")));
        assertEquals(List.of("a"), strict.entries(), "partial batch written by the inherited default");
        var atomic = new Item21InterfacesForPosterity.AtomicStrictJournal();
        assertThrows(IllegalArgumentException.class, () -> atomic.postAll(List.of("a", " ", "c")));
        assertEquals(List.of(), atomic.entries());
    }

    @Test
    void item22_constantInterfaceLeaksIntoTypeHierarchy() {
        assertTrue(Item22InterfacesOnlyForTypes.PaymentConstants.class
                .isAssignableFrom(Item22InterfacesOnlyForTypes.PaymentLimitsBad.class));
        assertEquals(140, Item22InterfacesOnlyForTypes.SepaLimits.MAX_REFERENCE_LENGTH);
    }

    @Test
    void item23_taggedAndSealedAgree() {
        var amount = new BigDecimal("200.00");
        var tagged = new Item23ClassHierarchies.TaggedFee(Item23ClassHierarchies.TaggedFee.Kind.TIERED,
                new BigDecimal("100"), BigDecimal.ONE, BigDecimal.TEN);
        assertEquals(tagged.feeFor(amount), Item23ClassHierarchies.feeFor(
                new Item23ClassHierarchies.Tiered(new BigDecimal("100"), BigDecimal.ONE, BigDecimal.TEN), amount));
        assertEquals(new BigDecimal("3.00"), Item23ClassHierarchies.feeFor(new Item23ClassHierarchies.Percentage(new BigDecimal("1.5")), amount));
    }

    @Test
    void item24_innerClassHoldsOuterReference() {
        assertTrue(Item24StaticMemberClasses.holdsOuterReference(Item24StaticMemberClasses.StatementGenerator.InnerLine.class));
        assertFalse(Item24StaticMemberClasses.holdsOuterReference(Item24StaticMemberClasses.StatementGenerator.Line.class));
    }

    @Test
    void item25_nestedTypesInsteadOfExtraTopLevelClasses() {
        Item25SingleTopLevelClass.SettlementStatus s = new Item25SingleTopLevelClass.Rejected("R1", "closed account");
        assertInstanceOf(Item25SingleTopLevelClass.Rejected.class, s);
        assertEquals(1, new HashSet<>(List.of(new Item25SingleTopLevelClass.Settled("x"), new Item25SingleTopLevelClass.Settled("x"))).size());
    }
}
