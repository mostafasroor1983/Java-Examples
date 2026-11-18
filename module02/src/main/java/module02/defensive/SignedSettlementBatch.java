package module02.defensive;

import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * A settlement file sent to a clearing house: a list of payment references plus a detached signature.
 *
 * <p>Two immutability traps in one record:
 * <ol>
 *   <li><b>List component</b>: copied with {@code List.copyOf} (also rejects null elements).</li>
 *   <li><b>Array component</b>: records compare arrays by <em>reference</em> and return the internal array from
 *   the accessor. Both are wrong for a value type, so this record clones in and out and overrides
 *   equals/hashCode/toString with {@link Arrays} methods.</li>
 * </ol>
 * {@link Instant} is already immutable, so no copy is needed (unlike {@code java.util.Date}).
 */
public record SignedSettlementBatch(String batchId, List<String> paymentRefs, Instant cutOff, byte[] signature) {
    public SignedSettlementBatch {
        Objects.requireNonNull(batchId, "batchId");
        Objects.requireNonNull(cutOff, "cutOff");
        Objects.requireNonNull(signature, "signature");
        paymentRefs = List.copyOf(paymentRefs);
        if (paymentRefs.isEmpty()) throw new IllegalArgumentException("a batch needs at least one payment");
        if (paymentRefs.stream().distinct().count() != paymentRefs.size()) {
            throw new IllegalArgumentException("duplicate payment reference in batch " + batchId);
        }
        signature = signature.clone(); // copy in: the caller's array can no longer change our state
    }

    /** Copy out: callers cannot tamper with the signature through the accessor. */
    @Override
    public byte[] signature() {
        return signature.clone();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SignedSettlementBatch b
                && batchId.equals(b.batchId)
                && paymentRefs.equals(b.paymentRefs)
                && cutOff.equals(b.cutOff)
                && Arrays.equals(signature, b.signature);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(batchId, paymentRefs, cutOff) + Arrays.hashCode(signature);
    }

    @Override
    public String toString() {
        return "SignedSettlementBatch[" + batchId + ", payments=" + paymentRefs.size() + ", cutOff=" + cutOff
                + ", sig=" + HexFormat.of().formatHex(signature, 0, Math.min(4, signature.length)) + "...]";
    }
}
