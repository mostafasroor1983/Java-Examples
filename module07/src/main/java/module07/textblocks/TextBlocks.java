package module07.textblocks;

import java.math.BigDecimal;

/** Text blocks: multi-line literals with incidental indentation stripped; formatted() for templating. */
public final class TextBlocks {
    private TextBlocks() {
    }

    public static final String STATEMENT_HEADER = """
            Account statement
            =================
            """;

    /** The closing delimiter position controls the indentation baseline. */
    public static String receiptJson(String paymentId, BigDecimal amount, String currency) {
        return """
                {
                  "paymentId": "%s",
                  "amount": "%s",
                  "currency": "%s"
                }
                """.formatted(paymentId, amount.toPlainString(), currency);
    }

    /** `\` at end of line joins lines; `\s` keeps a trailing space. */
    public static String sqlFindPayments() {
        return """
                SELECT id, amount, currency \
                FROM payments \
                WHERE status = ?\s\
                ORDER BY created_at DESC""";
    }

    public static String oldStyle(String paymentId) {
        return "{\n" +
                "  \"paymentId\": \"" + paymentId + "\"\n" +
                "}\n";
    }
}
