package module18;

import module18.ch06.*;
import module18.ch06.Item36EnumSet.Permission;
import module18.ch06.Item37EnumMap.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Chapter 6 - Enums and annotations (items 34-41). */
class Chapter06Test {

    @Test
    void item34_enumsCarryDataAndBehaviour() {
        assertEquals(Optional.of(Item34Enums.CardScheme.VISA), Item34Enums.CardScheme.fromPan("4111"));
        assertTrue(Item34Enums.CardScheme.fromPan("9").isEmpty());
        assertEquals(new BigDecimal("0.20"), Item34Enums.CardScheme.VISA.interchange(new BigDecimal("100")));
        assertEquals(new BigDecimal("0.50"), Item34Enums.CardScheme.AMEX.interchange(new BigDecimal("10")), "minimum fee override");
    }

    @Test
    void item35_ordinalIsNotBusinessData() {
        assertEquals(0, Item35InstanceFieldsNotOrdinals.SettlementCycleBad.T0.daysToSettle());
        assertEquals(0, Item35InstanceFieldsNotOrdinals.SettlementCycle.T0.daysToSettle());
        assertEquals(1, Item35InstanceFieldsNotOrdinals.SettlementCycle.T0.ordinal(), "ordinal shifted after adding INSTANT");
        assertEquals(2, Item35InstanceFieldsNotOrdinals.SettlementCycle.T2.daysToSettle());
    }

    @Test
    void item36_enumSetForPermissions() {
        assertEquals("[VIEW_BALANCES, INITIATE_PAYMENT]", Item36EnumSet.operatorRole().toString());
        assertTrue(Item36EnumSet.violatesSeparationOfDuties(Item36EnumSet.supervisorRole()));
        assertFalse(Item36EnumSet.violatesSeparationOfDuties(Item36EnumSet.operatorRole()));
        assertEquals(7, Item36EnumSet.VIEW | Item36EnumSet.INITIATE | Item36EnumSet.APPROVE, "what a bit field prints");
    }

    @Test
    void item37_enumMapGroupingAndTransitions() {
        var totals = Item37EnumMap.totalsByChannel(List.of(new Payment(Channel.SEPA, BigDecimal.TEN),
                new Payment(Channel.CARD, BigDecimal.ONE), new Payment(Channel.SEPA, BigDecimal.ONE)));
        assertInstanceOf(EnumMap.class, totals);
        assertEquals("{CARD=1, SEPA=11}", totals.toString(), "declaration order");
        assertEquals(State.SETTLED, Item37EnumMap.apply(State.AUTHORISED, Event.SETTLE));
        assertThrows(IllegalStateException.class, () -> Item37EnumMap.apply(State.SETTLED, Event.FAIL));
    }

    @Test
    void item38_interfaceExtendsEnums() {
        var amount = new BigDecimal("500");
        assertEquals(new BigDecimal("0.00"), Item38ExtensibleEnums.cheapest(Item38ExtensibleEnums.BasicFeeRule.class, amount));
        assertEquals(new BigDecimal("2.00"), Item38ExtensibleEnums.PromoFeeRule.CAPPED_TWO_EUR.apply(amount));
        assertEquals(new BigDecimal("2.00"), Item38ExtensibleEnums.cheapest(
                List.of(Item38ExtensibleEnums.BasicFeeRule.ONE_PERCENT, Item38ExtensibleEnums.PromoFeeRule.CAPPED_TWO_EUR), amount));
    }

    @Test
    void item39_annotationDrivenRedaction() {
        var h = new Item39Annotations.CardHolder("Jane", "4111111111111111", "123", "DE");
        assertEquals("CardHolder[name=Jane, pan=************1111, cvv=***, country=DE]", Item39Annotations.redactedToString(h));
    }

    @Test
    void item40_overloadedEqualsLetsDuplicatesThrough() {
        Set<Object> bad = new HashSet<>(List.of(new Item40Override.OverloadedPaymentId("P1"), new Item40Override.OverloadedPaymentId("P1")));
        assertEquals(2, bad.size());
        Set<Object> good = new HashSet<>(List.of(new Item40Override.PaymentId("P1"), new Item40Override.PaymentId("P1")));
        assertEquals(1, good.size());
    }

    @Test
    void item41_markerInterfaceIsCompileTimeChecked() {
        var vault = new Item41MarkerInterfaces.Vault();
        vault.storeInVault(new Item41MarkerInterfaces.CardData("tok_123", "12/30"));
        // vault.storeInVault(new Item41MarkerInterfaces.Address("x", "DE"));  <- does not compile
        assertEquals(1, vault.size());
        assertFalse(Item41MarkerInterfaces.PciScoped.class.isAssignableFrom(Item41MarkerInterfaces.Address.class));
    }
}
