package module18.ch12;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;

/** Small helpers used by the chapter 12 examples and tests. */
public final class SerializationSupport {
    private SerializationSupport() {
    }

    public static byte[] serialize(Object o) {
        try (var bytes = new ByteArrayOutputStream(); var out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Deserializes with an optional filter (null = no filter: DANGEROUS for untrusted input). */
    public static Object deserialize(byte[] data, ObjectInputFilter filter) throws IOException, ClassNotFoundException {
        try (var in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            if (filter != null) in.setObjectInputFilter(filter);
            return in.readObject();
        }
    }
}
