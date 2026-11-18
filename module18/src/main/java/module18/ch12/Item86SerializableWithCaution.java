package module18.ch12;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Item 86 - Implement Serializable with great caution.
 *
 * <p>Implementing Serializable makes the private representation part of the public API forever, adds a hidden
 * constructor (deserialization bypasses yours, so invariants are not checked), increases security and testing burden,
 * and couples releases. If you do it: declare an explicit {@code serialVersionUID} (otherwise any change, even adding
 * a method, can change the computed UID and break compatibility), use {@link Serial} (Java 14) on serialization
 * members so the compiler checks them, and never make inner classes serializable. Records (Java 16) are the safer
 * option: they serialize by component and deserialize THROUGH the canonical constructor.
 */
public final class Item86SerializableWithCaution {
    private Item86SerializableWithCaution() {
    }

    public static final class SettlementRecord implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String batchId;
        private final BigDecimal total;
        private final LocalDate valueDate;

        public SettlementRecord(String batchId, BigDecimal total, LocalDate valueDate) {
            this.batchId = batchId;
            this.total = total;
            this.valueDate = valueDate;
        }

        public String batchId() { return batchId; }

        public BigDecimal total() { return total; }

        public LocalDate valueDate() { return valueDate; }
    }

    /** Record: deserialization runs this compact constructor, so the invariant is enforced on the way in too. */
    public record SettlementSummary(String batchId, BigDecimal total) implements Serializable {
        public SettlementSummary {
            if (total.signum() < 0) throw new IllegalArgumentException("total must be >= 0");
        }
    }
}
