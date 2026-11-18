package module18.ch03;

import java.math.BigDecimal;

/**
 * Item 12 - Always override toString.
 *
 * <p>A good toString makes logs and test failures useful ({@code Transfer[ref=T-1, from=****3000, ...]} instead of
 * {@code Transfer@1b6d3586}). In fintech it has a second job: it must NOT leak sensitive data, because toString output
 * ends up in logs, traces and exception messages. Record toString prints every component, so override it when a
 * component is sensitive. Document whether the format is stable (machine-parseable) or not; here it is not.
 */
public final class Item12ToString {
    private Item12ToString() {
    }

    public record Transfer(String reference, String fromIban, String toIban, BigDecimal amount, String currency) {
        @Override
        public String toString() {
            return "Transfer[ref=" + reference + ", from=" + mask(fromIban) + ", to=" + mask(toIban)
                    + ", amount=" + amount.toPlainString() + " " + currency + "]";
        }

        private static String mask(String iban) {
            return iban.length() <= 4 ? "****" : "****" + iban.substring(iban.length() - 4);
        }
    }
}
