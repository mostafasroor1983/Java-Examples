package module18.ch12;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Item 87 - Consider using a custom serialized form.
 *
 * <p>The default form writes the PHYSICAL representation (every node of an internal linked structure, every cache
 * field), which ties you to it forever and can be slow or overflow the stack. Write the LOGICAL content instead:
 * mark implementation fields {@code transient}, and write a compact form in {@code writeObject}/{@code readObject}.
 * Here the transaction log stores only the entry count and the entries; the derived running total is recomputed.
 */
public final class Item87CustomSerializedForm {
    private Item87CustomSerializedForm() {
    }

    public static final class TransactionLog implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private transient List<Long> entriesCents = new ArrayList<>();
        private transient long runningTotalCents; // derived: not serialized

        public void append(long cents) {
            entriesCents.add(cents);
            runningTotalCents += cents;
        }

        public long total() {
            return runningTotalCents;
        }

        public int size() {
            return entriesCents.size();
        }

        @Serial
        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();      // future-proofing, even with no non-transient fields
            out.writeInt(entriesCents.size());
            for (long c : entriesCents) out.writeLong(c);
        }

        @Serial
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            int n = in.readInt();
            if (n < 0 || n > 1_000_000) throw new java.io.InvalidObjectException("bad entry count " + n);
            entriesCents = new ArrayList<>(n);
            runningTotalCents = 0;
            for (int i = 0; i < n; i++) append(in.readLong()); // rebuild derived state
        }
    }
}
