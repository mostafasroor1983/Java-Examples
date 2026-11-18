package module18.ch04;

import java.util.ArrayList;
import java.util.List;

/**
 * Item 24 - Favor static member classes over nonstatic.
 *
 * <p>A non-static (inner) class instance holds a hidden reference to its enclosing instance. If inner instances
 * outlive the outer one (handed to a cache, a listener list, a thread), they keep the whole outer object alive: a
 * memory leak. Use {@code static} nested classes unless you truly need the enclosing instance.
 *
 * <p>Java version note: since JDK 18, javac omits the synthetic {@code this$0} field when an inner class never uses
 * its enclosing instance (JDK-8271623). As soon as the inner class touches the outer instance, as {@code InnerLine}
 * does below and as real code usually does, the reference and the leak are back. Do not rely on the optimisation:
 * declare nested classes {@code static}.
 */
public final class Item24StaticMemberClasses {
    private Item24StaticMemberClasses() {
    }

    public static final class StatementGenerator {
        private final byte[] largeBuffer = new byte[1024 * 1024]; // kept alive by every Inner line
        private final List<Object> lines = new ArrayList<>();

        /** BAD for long-lived objects: implicit StatementGenerator.this reference. */
        public final class InnerLine {
            final String text;

            InnerLine(String text) {
                this.text = text;
            }

            /** Uses the enclosing instance, so javac must keep the hidden reference. */
            int generatorBufferSize() {
                return bufferSize();
            }
        }

        /** GOOD: no hidden reference. */
        public static final class Line {
            final String text;

            Line(String text) {
                this.text = text;
            }
        }

        public InnerLine innerLine(String t) {
            return new InnerLine(t);
        }

        public Line line(String t) {
            return new Line(t);
        }

        int bufferSize() {
            return largeBuffer.length + lines.size();
        }
    }

    /** True if instances of the class carry a synthetic reference to an enclosing instance. */
    public static boolean holdsOuterReference(Class<?> c) {
        for (var f : c.getDeclaredFields()) if (f.isSynthetic() && f.getName().startsWith("this$")) return true;
        return false;
    }
}
