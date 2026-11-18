package module18;

import module18.ch12.*;
import org.junit.jupiter.api.Test;

import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;

import static module18.ch12.SerializationSupport.deserialize;
import static module18.ch12.SerializationSupport.serialize;
import static org.junit.jupiter.api.Assertions.*;

/** Chapter 12 - Serialization (items 85-90). */
class Chapter12Test {

    @Test
    void item85_allowListFilterRejectsUnexpectedClasses() throws Exception {
        var msg = new Item85AlternativesToSerialization.PaymentMessage("P1", new BigDecimal("10.00"), "EUR");
        assertEquals(msg, deserialize(serialize(msg), Item85AlternativesToSerialization.allowListFilter()));
        var gadgetStandIn = new HashMap<String, String>(); // anything not on the allow-list
        assertThrows(InvalidClassException.class, () -> deserialize(serialize(gadgetStandIn), Item85AlternativesToSerialization.allowListFilter()));
        assertEquals("{\"id\":\"P1\",\"amount\":\"10.00\",\"currency\":\"EUR\"}", Item85AlternativesToSerialization.toJson(msg));
    }

    @Test
    void item86_explicitUidAndRecordValidation() throws Exception {
        var r = new Item86SerializableWithCaution.SettlementRecord("B1", BigDecimal.TEN, LocalDate.of(2024, 3, 1));
        var copy = (Item86SerializableWithCaution.SettlementRecord) deserialize(serialize(r), null);
        assertEquals("B1", copy.batchId());
        assertEquals(1L, java.io.ObjectStreamClass.lookup(Item86SerializableWithCaution.SettlementRecord.class).getSerialVersionUID());
        var summary = new Item86SerializableWithCaution.SettlementSummary("B1", BigDecimal.ONE);
        assertEquals(summary, deserialize(serialize(summary), null));
    }

    @Test
    void item87_customFormRebuildsDerivedState() throws Exception {
        var log = new Item87CustomSerializedForm.TransactionLog();
        log.append(1_000);
        log.append(-250);
        var copy = (Item87CustomSerializedForm.TransactionLog) deserialize(serialize(log), null);
        assertEquals(2, copy.size());
        assertEquals(750, copy.total());
    }

    @Test
    void item88_readObjectRevalidatesTamperedState() throws Exception {
        var period = new Item88DefensiveReadObject.InterestPeriod(new Date(0), new Date(1_000));
        var copy = (Item88DefensiveReadObject.InterestPeriod) deserialize(serialize(period), null);
        assertEquals(new Date(1_000), copy.end());

        // Simulate a forged stream: break the invariant behind the constructor's back, then serialize.
        var forged = new Item88DefensiveReadObject.InterestPeriod(new Date(0), new Date(1_000));
        var startField = Item88DefensiveReadObject.InterestPeriod.class.getDeclaredField("start");
        startField.setAccessible(true);
        startField.set(forged, new Date(5_000));
        byte[] bytes = serialize(forged);
        assertThrows(InvalidObjectException.class, () -> deserialize(bytes, null));
    }

    @Test
    void item89_enumKeepsSingletonPropertyReadResolveToo() throws Exception {
        assertSame(Item89EnumOverReadResolve.SystemAccount.SUSPENSE,
                deserialize(serialize(Item89EnumOverReadResolve.SystemAccount.SUSPENSE), null));
        assertSame(Item89EnumOverReadResolve.SuspenseAccount.INSTANCE,
                deserialize(serialize(Item89EnumOverReadResolve.SuspenseAccount.INSTANCE), null));
    }

    @Test
    void item90_proxyRoundTripsThroughPublicConstructor() throws Exception {
        var m = new Item90SerializationProxy.Mandate("MNDT-2024-001", "DE89370400440532013000", new BigDecimal("250.00"));
        var copy = (Item90SerializationProxy.Mandate) deserialize(serialize(m), null);
        assertEquals("MNDT-2024-001", copy.mandateId());
        assertEquals(new BigDecimal("250.00"), copy.maxAmount());
        assertThrows(IllegalArgumentException.class, () -> new Item90SerializationProxy.Mandate("M1", "DE", BigDecimal.ZERO));
    }
}
