package module18;

import module18.ch07.*;
import module18.ch07.Item44StandardFunctionalInterfaces.Txn;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Chapter 7 - Lambdas and streams (items 42-48). */
class Chapter07Test {

    @Test
    void item42_lambdaAndAnonymousClassAgree() {
        var a = new ArrayList<>(List.of(new Item42Lambdas.Payment("1", BigDecimal.TEN), new Item42Lambdas.Payment("2", BigDecimal.ONE)));
        var b = new ArrayList<>(a);
        Item42Lambdas.sortByAmountAnonymous(a);
        Item42Lambdas.sortByAmount(b);
        assertEquals(a, b);
        var x = new BigDecimal("1.005");
        assertEquals(new BigDecimal("1.00"), Item42Lambdas.RoundingPolicy.BANKERS.round(x));
        assertEquals(new BigDecimal("1.00"), Item42Lambdas.RoundingPolicy.HOUSE_NEVER_LOSES.round(x));
        assertEquals(new BigDecimal("1.01"), Item42Lambdas.RoundingPolicy.CUSTOMER_FRIENDLY.round(x));
    }

    @Test
    void item43_methodReferences() {
        var balances = Item43MethodReferences.balances(List.of(
                new Item43MethodReferences.Posting("A", BigDecimal.TEN), new Item43MethodReferences.Posting("A", BigDecimal.ONE)));
        assertEquals(new BigDecimal("11"), balances.get("A"));
        assertEquals(List.of("INV-1", "INV-2"), Item43MethodReferences.normalisedRefs(List.of(" inv-1 ", "", "inv-2")));
    }

    @Test
    void item44_standardInterfacesCompose() {
        var now = Instant.EPOCH;
        var txns = List.of(new Txn("1", new BigDecimal("5000"), "DE", now), new Txn("2", new BigDecimal("50"), "XX", now),
                new Txn("3", new BigDecimal("9000"), "XX", now));
        var rule = Item44StandardFunctionalInterfaces.largeAmount(new BigDecimal("1000"))
                .and(Item44StandardFunctionalInterfaces.fromCountry("DE").negate());
        assertEquals(List.of("3"), Item44StandardFunctionalInterfaces.flagged(txns, rule).stream().map(Txn::id).toList());
        Item44StandardFunctionalInterfaces.FraudRule velocity = t -> t.amount().signum() > 0 ? null : "non-positive";
        assertNull(velocity.evaluate(txns.get(0)));
        assertEquals(now, Item44StandardFunctionalInterfaces.stamp(() -> now));
    }

    @Test
    void item45_streamOrLoopWhicheverReadsBetter() {
        var txns = List.of(new Item45StreamsJudiciously.CardTxn("shop", BigDecimal.TEN),
                new Item45StreamsJudiciously.CardTxn("cafe", BigDecimal.ONE), new Item45StreamsJudiciously.CardTxn("shop", BigDecimal.TEN));
        assertEquals("{cafe=1, shop=20}", Item45StreamsJudiciously.spendByMerchant(txns).toString());
        assertEquals(2, Item45StreamsJudiciously.untilBudgetExceeded(txns, new BigDecimal("15")).size());
    }

    @Test
    void item46_collectorsInsteadOfForEachMutation() {
        var reasons = List.of("SOFT_FUNDS", "HARD_STOLEN", "SOFT_FUNDS");
        assertEquals(Item46SideEffectFreeStreams.declinesByReasonBad(reasons), Item46SideEffectFreeStreams.declinesByReason(reasons));
        assertEquals(List.of("HARD_STOLEN"), Item46SideEffectFreeStreams.partitionHardDeclines(reasons).get(true));
    }

    @Test
    void item47_collectionSupportsBothStylesAndAdapters() {
        var lines = Item47CollectionReturnType.statementLines();
        int n = 0;
        for (var l : lines) n++;
        assertEquals(n, lines.stream().count());
        int m = 0;
        for (var l : Item47CollectionReturnType.iterableOf(lines.stream())) m++;
        assertEquals(2, m);
        assertEquals(2, Item47CollectionReturnType.streamOf(lines).count());
    }

    @Test
    void item48_parallelOnlyWithAssociativeSideEffectFreeOps() {
        var amounts = Collections.nCopies(10_000, new BigDecimal("0.01"));
        assertEquals(new BigDecimal("100.00"), Item48ParallelStreams.totalParallel(amounts));
        assertEquals(100_000, Item48ParallelStreams.collectProperly(100_000));
        assertTrue(Item48ParallelStreams.collectWithSideEffect(100_000) <= 100_000, "unreliable: may lose elements");
    }
}
