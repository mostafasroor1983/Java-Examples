package module08.redaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogSanitizerTest {

    @Test
    void masksLuhnValidPansOnly() {
        assertEquals("card ****1111 declined", LogSanitizer.sanitize("card 4111 1111 1111 1111 declined"));
        assertEquals("card ****4444", LogSanitizer.sanitize("card 5555-5555-5555-4444"));
        assertEquals("order 1234567890123 ok", LogSanitizer.sanitize("order 1234567890123 ok"), "not Luhn-valid: untouched");
    }

    @Test
    void masksIbanAndEmail() {
        assertEquals("from DE89****3000 to ***@example.com",
                LogSanitizer.sanitize("from DE89370400440532013000 to jane.doe@example.com"));
    }

    @Test
    void neutralisesLogInjection() {
        String s = LogSanitizer.sanitize("ref=abc\nINFO payment approved");
        assertFalse(s.contains("\n"));
        assertEquals("ref=abc\\nINFO payment approved", s);
        assertNull(LogSanitizer.sanitize(null));
    }

    @Test
    void describesWholeCauseChain() {
        var e = new IllegalStateException("charge failed",
                new IllegalArgumentException("bad card 4111111111111111 for jane@example.com"));
        assertEquals("IllegalStateException: charge failed <- IllegalArgumentException: bad card ****1111 for ***@example.com",
                LogSanitizer.describe(e));
    }
}
