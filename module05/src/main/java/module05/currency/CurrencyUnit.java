package module05.currency;

/**
 * Currencies as TYPES. Each currency is a singleton class, so {@code Amount<EUR>} and {@code Amount<USD>} are
 * different static types and the compiler rejects {@code eur.plus(usd)}. Sealed: the set of supported currencies
 * is closed and reviewed (adding one is a deliberate change, not a typo).
 */
public sealed interface CurrencyUnit permits CurrencyUnit.EUR, CurrencyUnit.USD, CurrencyUnit.GBP, CurrencyUnit.JPY {
    String code();

    int scale();

    final class EUR implements CurrencyUnit {
        public static final EUR INSTANCE = new EUR();
        private EUR() { }
        public String code() { return "EUR"; }
        public int scale() { return 2; }
    }

    final class USD implements CurrencyUnit {
        public static final USD INSTANCE = new USD();
        private USD() { }
        public String code() { return "USD"; }
        public int scale() { return 2; }
    }

    final class GBP implements CurrencyUnit {
        public static final GBP INSTANCE = new GBP();
        private GBP() { }
        public String code() { return "GBP"; }
        public int scale() { return 2; }
    }

    final class JPY implements CurrencyUnit {
        public static final JPY INSTANCE = new JPY();
        private JPY() { }
        public String code() { return "JPY"; }
        public int scale() { return 0; }
    }
}
