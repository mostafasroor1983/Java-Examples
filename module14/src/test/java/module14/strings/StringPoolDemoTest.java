package module14.strings;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringPoolDemoTest {

    @Test
    void poolSemantics() {
        assertTrue(StringPoolDemo.literalsShareInstance());
        assertFalse(StringPoolDemo.newStringIsDistinct());
        assertTrue(StringPoolDemo.internRestoresSharing());
        assertTrue(StringPoolDemo.constantFolding());
        assertFalse(StringPoolDemo.runtimeConcatNotPooled("GB"));
    }

    @Test
    void joiningAndMasking() {
        List<String> ids = List.of("a", "b", "c");
        assertEquals(StringPoolDemo.joinBad(ids), StringPoolDemo.joinGood(ids));
        assertEquals("a,b,c,", StringPoolDemo.joinGood(ids));
        assertEquals("411111******1111", StringPoolDemo.maskPan("4111111111111111"));
        assertThrows(IllegalArgumentException.class, () -> StringPoolDemo.maskPan("1234"));
    }
}
