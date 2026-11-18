package module18.ch12;

import java.io.ObjectInputFilter;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Item 85 - Prefer alternatives to Java serialization.
 *
 * <p>Deserializing untrusted bytes can execute "gadget chains" from classes on the classpath (remote code execution)
 * or exhaust memory/CPU ("deserialization bombs"). The best defence is to never deserialize untrusted data: use
 * cross-platform structured formats (JSON, Protocol Buffers, Avro) mapped to explicit DTOs. If you cannot avoid it,
 * install an ALLOW-LIST {@link ObjectInputFilter} (Java 9, JEP 290; JVM-wide filter factories since Java 17, JEP 415)
 * with limits on depth, references and array sizes. Deny-lists are never complete.
 */
public final class Item85AlternativesToSerialization {
    private Item85AlternativesToSerialization() {
    }

    /** A message DTO that would normally travel as JSON/Protobuf; Serializable here only to demonstrate filtering. */
    public record PaymentMessage(String id, BigDecimal amount, String currency) implements java.io.Serializable {
        public PaymentMessage {
            Objects.requireNonNull(id);
            Objects.requireNonNull(amount);
            Objects.requireNonNull(currency);
        }
    }

    /** Allow-list: only our DTO and the JDK value types it contains; everything else rejected ("!*"). */
    public static ObjectInputFilter allowListFilter() {
        return ObjectInputFilter.Config.createFilter(
                "maxdepth=5;maxrefs=50;maxarray=1000;"
                        + PaymentMessage.class.getName() + ";java.math.BigDecimal;java.math.BigInteger;java.lang.Number;!*");
    }

    /** What a JSON encoding of the same message looks like (hand-written to stay dependency-free). */
    public static String toJson(PaymentMessage m) {
        return "{\"id\":\"" + m.id() + "\",\"amount\":\"" + m.amount().toPlainString() + "\",\"currency\":\"" + m.currency() + "\"}";
    }
}
