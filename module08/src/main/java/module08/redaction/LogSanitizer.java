package module08.redaction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Last line of defence before a message reaches logs, traces or error trackers.
 *
 * <ul>
 *   <li><b>PANs</b> (13-19 digits, optionally separated by spaces/dashes) that pass Luhn are masked to the last 4
 *   digits. The Luhn check avoids masking order numbers and timestamps that merely look long.</li>
 *   <li><b>IBANs</b> keep the country code and last 4 characters.</li>
 *   <li><b>E-mail addresses</b> keep only the domain.</li>
 *   <li><b>CR/LF</b> are escaped to stop log injection (forged log lines such as {@code \nINFO payment approved}).</li>
 * </ul>
 *
 * <p>Redaction is a safety net, not a strategy: the primary control is to never put sensitive values in messages
 * (see {@code validation.PaymentRequestValidator}) and to tokenise card data at the edge.
 */
public final class LogSanitizer {
    private static final Pattern PAN = Pattern.compile("(?<!\\d)(?:\\d[ -]?){12,18}\\d(?!\\d)");
    private static final Pattern IBAN = Pattern.compile("\\b([A-Z]{2}\\d{2})((?:\\s?[A-Z0-9]){7,26})([A-Z0-9]{4})\\b");
    private static final Pattern EMAIL = Pattern.compile("\\b[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");

    private LogSanitizer() {
    }

    public static String sanitize(String message) {
        if (message == null) return null;
        String out = message.replace("\r", "\\r").replace("\n", "\\n");
        out = replace(PAN, out, m -> {
            String digits = m.group().replaceAll("[ -]", "");
            return luhn(digits) ? "****" + digits.substring(digits.length() - 4) : m.group();
        });
        out = replace(IBAN, out, m -> m.group(1) + "****" + m.group(3));
        out = replace(EMAIL, out, m -> "***@" + m.group(1));
        return out;
    }

    /** Sanitises an exception chain into one line (type + sanitised message per cause) for logging. */
    public static String describe(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (sb.length() > 0) sb.append(" <- ");
            sb.append(c.getClass().getSimpleName()).append(": ").append(sanitize(String.valueOf(c.getMessage())));
            if (c.getCause() == c) break;
        }
        return sb.toString();
    }

    private static String replace(Pattern p, String in, java.util.function.Function<Matcher, String> f) {
        Matcher m = p.matcher(in);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(f.apply(m)));
        m.appendTail(sb);
        return sb.toString();
    }

    static boolean luhn(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl && (d *= 2) > 9) d -= 9;
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }
}
