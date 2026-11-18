package module07.messages;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Renders a simplified ISO 20022 pacs.008-style credit transfer with a text block (Java 15+, used here as Java 17).
 *
 * <p>Security: text blocks are just strings. Interpolating untrusted data (debtor name, remittance info) into XML
 * without escaping allows XML injection: a name like {@code </Nm><Amt>999999</Amt><Nm>} would forge elements.
 * Every free-text field is escaped, and amounts are formatted with {@code toPlainString()} (never scientific notation
 * such as {@code 1E+3}, which downstream parsers may reject or misread).
 *
 * <p>Production systems generate these messages with a schema-validated XML binder (JAXB, StAX) and validate against
 * the official XSD. This class shows the failure mode, not a replacement for proper marshalling.
 */
public final class CreditTransferMessage {
    private static final String TEMPLATE = """
            <CdtTrfTxInf>
              <PmtId><EndToEndId>%s</EndToEndId></PmtId>
              <IntrBkSttlmAmt Ccy="%s">%s</IntrBkSttlmAmt>
              <IntrBkSttlmDt>%s</IntrBkSttlmDt>
              <Dbtr><Nm>%s</Nm></Dbtr>
              <Cdtr><Nm>%s</Nm></Cdtr>
              <RmtInf><Ustrd>%s</Ustrd></RmtInf>
            </CdtTrfTxInf>
            """;

    private CreditTransferMessage() {
    }

    public record Transfer(String endToEndId, String currency, BigDecimal amount, LocalDate settlementDate,
                           String debtorName, String creditorName, String remittanceInfo) {
        public Transfer {
            Objects.requireNonNull(endToEndId);
            Objects.requireNonNull(currency);
            Objects.requireNonNull(amount);
            Objects.requireNonNull(settlementDate);
            if (!endToEndId.matches("[A-Za-z0-9-]{1,35}")) throw new IllegalArgumentException("invalid EndToEndId");
            if (!currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("invalid currency");
            if (amount.signum() <= 0 || amount.scale() > 2) throw new IllegalArgumentException("invalid amount");
        }
    }

    public static String render(Transfer t) {
        return TEMPLATE.formatted(t.endToEndId(), t.currency(), t.amount().toPlainString(), t.settlementDate(),
                xml(t.debtorName(), 140), xml(t.creditorName(), 140), xml(t.remittanceInfo(), 140));
    }

    /** Escapes XML special characters and truncates to the scheme's field length. */
    static String xml(String s, int maxLen) {
        if (s == null) return "";
        String trimmed = s.strip();
        if (trimmed.length() > maxLen) trimmed = trimmed.substring(0, maxLen);
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (char c : trimmed.toCharArray()) {
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> {
                    if (c >= 0x20 || c == '\t') sb.append(c); // drop control characters (invalid in XML 1.0)
                }
            }
        }
        return sb.toString();
    }
}
