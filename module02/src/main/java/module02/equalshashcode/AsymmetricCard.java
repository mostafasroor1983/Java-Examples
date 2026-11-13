package module02.equalshashcode;

/**
 * BAD: uses instanceof against a subclass-friendly type but compares a subclass field only on one side,
 * breaking symmetry: a.equals(b) != b.equals(a).
 */
public class AsymmetricCard {
    protected final String pan;

    public AsymmetricCard(String pan) {
        this.pan = pan;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AsymmetricCard)) return false;
        return pan.equals(((AsymmetricCard) o).pan);
    }

    @Override
    public int hashCode() {
        return pan.hashCode();
    }

    public static class SchemeCard extends AsymmetricCard {
        private final String scheme;

        public SchemeCard(String pan, String scheme) {
            super(pan);
            this.scheme = scheme;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SchemeCard)) return false;
            SchemeCard other = (SchemeCard) o;
            return pan.equals(other.pan) && scheme.equals(other.scheme);
        }

        @Override
        public int hashCode() {
            return 31 * pan.hashCode() + scheme.hashCode();
        }
    }
}
