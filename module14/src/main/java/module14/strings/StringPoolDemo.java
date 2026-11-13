package module14.strings;

/**
 * String pool: compile-time constants and intern()ed strings live in one table; `==` compares references.
 * Strings are immutable; concatenation in loops allocates; StringBuilder or String.join avoids it.
 * Since JDK 9 strings are Latin-1 compacted (1 byte/char when possible).
 */
public final class StringPoolDemo {
    private StringPoolDemo() {
    }

    public static boolean literalsShareInstance() {
        String a = "GBP";
        String b = "GBP";
        return a == b;
    }

    public static boolean newStringIsDistinct() {
        String a = "GBP";
        String b = new String("GBP");
        return a == b; // false
    }

    public static boolean internRestoresSharing() {
        String a = "GBP";
        String b = new String("GBP").intern();
        return a == b; // true
    }

    /** Compile-time constant folding: "GB" + "P" is folded to "GBP" and pooled. */
    public static boolean constantFolding() {
        final String prefix = "GB";
        String folded = prefix + "P";
        return folded == "GBP";
    }

    /** Runtime concatenation is not pooled. */
    public static boolean runtimeConcatNotPooled(String prefix) {
        String built = prefix + "P";
        return built == "GBP";
    }

    /** BAD: O(n^2) copying. */
    public static String joinBad(java.util.List<String> ids) {
        String s = "";
        for (String id : ids) s += id + ",";
        return s;
    }

    /** GOOD */
    public static String joinGood(java.util.List<String> ids) {
        StringBuilder sb = new StringBuilder(ids.size() * 8);
        for (String id : ids) sb.append(id).append(',');
        return sb.toString();
    }

    /** Masking PANs without creating a chain of intermediate strings. */
    public static String maskPan(String pan) {
        if (pan == null || pan.length() < 8) throw new IllegalArgumentException("pan too short");
        char[] out = pan.toCharArray();
        for (int i = 6; i < out.length - 4; i++) out[i] = '*';
        return new String(out);
    }
}
