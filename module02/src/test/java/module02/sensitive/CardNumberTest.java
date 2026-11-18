package module02.sensitive;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CardNumberTest {
    private static final String VISA_TEST = "4111 1111 1111 1111"; // public test PAN, Luhn-valid

    @Test
    void toStringNeverContainsTheFullPan() {
        CardNumber c = new CardNumber(VISA_TEST);
        assertEquals("************1111", c.masked());
        assertFalse(c.toString().contains("4111111111111111"));
        assertEquals("CardNumber[************1111]", c.toString());
        assertFalse(String.valueOf(Map.of("card", c)).contains("411111111111"), "also safe when nested in collections");
    }

    @Test
    void validationRejectsBadInputWithoutEchoingIt() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new CardNumber("4111111111111112"));
        assertFalse(ex.getMessage().contains("4111"));
        assertThrows(IllegalArgumentException.class, () -> new CardNumber("4111-1111-1111-1111"));
        assertThrows(IllegalArgumentException.class, () -> new CardNumber("12345"));
        assertThrows(NullPointerException.class, () -> new CardNumber(null));
    }

    @Test
    void valueEqualityIgnoresFormatting() {
        assertEquals(new CardNumber(VISA_TEST), new CardNumber("4111111111111111"));
        assertEquals(new CardNumber(VISA_TEST).hashCode(), new CardNumber("4111111111111111").hashCode());
        assertTrue(new CardNumber(VISA_TEST).matches("4111111111111111"));
        assertFalse(new CardNumber(VISA_TEST).matches("5555555555554444"));
        assertFalse(new CardNumber(VISA_TEST).matches(null));
    }
}
