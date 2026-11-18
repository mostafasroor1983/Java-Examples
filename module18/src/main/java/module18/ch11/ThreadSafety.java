package module18.ch11;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documentation annotations for Item 82 (in the spirit of JCIP's annotations; defined here to stay dependency-free).
 * They have no runtime effect: they make the thread-safety contract visible in code and in Javadoc.
 */
public final class ThreadSafety {
    private ThreadSafety() {
    }

    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    public @interface ThreadSafe { }

    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    public @interface NotThreadSafe { }

    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    public @interface Immutable { }

    /** The lock that guards a field, e.g. {@code @GuardedBy("lock")}. */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface GuardedBy {
        String value();
    }
}
