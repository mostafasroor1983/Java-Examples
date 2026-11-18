package module18.ch06;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.RecordComponent;
import java.util.StringJoiner;

/**
 * Item 39 - Prefer annotations to naming patterns.
 *
 * <p>Naming patterns ("fields starting with secret are sensitive") fail silently on typos and cannot carry parameters.
 * An annotation is checked by the compiler, can target exactly the right elements and carry values. Here
 * {@code @Sensitive} marks record components, and a generic redactor uses the Java 16+ record reflection API
 * ({@link RecordComponent}) to mask them. Unannotated components print normally.
 */
public final class Item39Annotations {
    private Item39Annotations() {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD})
    public @interface Sensitive {
        /** Number of trailing characters left visible. */
        int keepLast() default 0;
    }

    public record CardHolder(String name, @Sensitive(keepLast = 4) String pan, @Sensitive String cvv, String country) { }

    public static String redactedToString(Record r) {
        StringJoiner j = new StringJoiner(", ", r.getClass().getSimpleName() + "[", "]");
        for (RecordComponent c : r.getClass().getRecordComponents()) {
            Object value;
            try {
                value = c.getAccessor().invoke(r);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            Sensitive s = c.getAnnotation(Sensitive.class);
            String text = String.valueOf(value);
            if (s != null) {
                int keep = Math.min(s.keepLast(), text.length());
                text = "*".repeat(text.length() - keep) + text.substring(text.length() - keep);
            }
            j.add(c.getName() + "=" + text);
        }
        return j.toString();
    }
}
