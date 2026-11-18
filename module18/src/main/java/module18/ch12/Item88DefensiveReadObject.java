package module18.ch12;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Item 88 - Write readObject methods defensively.
 *
 * <p>{@code readObject} is effectively a public constructor that takes a byte stream an attacker controls. It must
 * (1) defensively copy any mutable component (otherwise the attacker keeps a reference into your object) and
 * (2) re-check every invariant, throwing {@link InvalidObjectException}. Never call overridable methods from it.
 * Note: fields cannot be final if readObject must assign copies, which is one more reason to prefer the
 * serialization proxy (Item 90) or a record.
 */
public final class Item88DefensiveReadObject {
    private Item88DefensiveReadObject() {
    }

    /** Value-dated interest period with legacy Date fields. */
    public static final class InterestPeriod implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Date start;
        private Date end;

        public InterestPeriod(Date start, Date end) {
            this.start = new Date(start.getTime());
            this.end = new Date(end.getTime());
            check();
        }

        private void check() throws IllegalArgumentException {
            if (start.after(end)) throw new IllegalArgumentException(start + " after " + end);
        }

        public Date start() { return new Date(start.getTime()); }

        public Date end() { return new Date(end.getTime()); }

        @Serial
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            start = new Date(start.getTime()); // defensive copies FIRST
            end = new Date(end.getTime());
            if (start.after(end)) throw new InvalidObjectException("start after end"); // then validate
        }
    }
}
