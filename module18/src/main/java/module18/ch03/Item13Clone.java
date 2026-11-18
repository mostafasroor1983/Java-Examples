package module18.ch03;

import java.util.Arrays;

/**
 * Item 13 - Override clone judiciously.
 *
 * <p>{@code Cloneable} is a broken mixin: clone() is protected on Object, makes shallow copies by default, bypasses
 * constructors and does not work with final fields. If you must, deep-copy mutable state (here the limits array).
 * Prefer a copy constructor or a static copy factory: explicit, works with final fields, no checked exceptions.
 * Arrays are the one place where {@code array.clone()} is the idiomatic copy.
 */
public final class Item13Clone {
    private Item13Clone() {
    }

    /** Daily limits per channel (index = channel). Implements clone correctly, for comparison. */
    public static final class ExposureLimits implements Cloneable {
        private long[] limitsMinor;

        public ExposureLimits(long... limitsMinor) {
            this.limitsMinor = limitsMinor.clone();
        }

        /** Preferred alternative: copy factory. */
        public static ExposureLimits copyOf(ExposureLimits other) {
            return new ExposureLimits(other.limitsMinor);
        }

        @Override
        public ExposureLimits clone() {
            try {
                ExposureLimits copy = (ExposureLimits) super.clone();
                copy.limitsMinor = limitsMinor.clone(); // deep copy: otherwise both share one array
                return copy;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e); // cannot happen: we implement Cloneable
            }
        }

        public void setLimit(int channel, long minor) {
            limitsMinor[channel] = minor;
        }

        public long limit(int channel) {
            return limitsMinor[channel];
        }

        @Override
        public String toString() {
            return Arrays.toString(limitsMinor);
        }
    }
}
