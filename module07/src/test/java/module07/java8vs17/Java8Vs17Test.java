package module07.java8vs17;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class Java8Vs17Test {

    @Test
    void collectionsFactoriesAreEquivalentButStricter() {
        assertEquals(Java8Vs17.currenciesOld(), Java8Vs17.currenciesNew());
        assertEquals(Java8Vs17.decimalsOld(), Java8Vs17.decimalsNew());
        assertThrows(UnsupportedOperationException.class, () -> Java8Vs17.currenciesNew().add("USD"));
        assertThrows(NullPointerException.class, () -> List.of("a", null));
        assertThrows(UnsupportedOperationException.class, () -> Java8Vs17.asListOld("a").add("b"));
    }

    @Test
    void varOptionalStreams() {
        assertEquals(Java8Vs17.sumLengthsOld(List.of("ab", "c")), Java8Vs17.sumLengthsNew(List.of("ab", "c")));
        assertEquals("anonymous", Java8Vs17.nameOrDefaultOld(Optional.empty()));
        assertEquals("bob", Java8Vs17.nameOrDefaultNew(Optional.empty(), Optional.of("bob")));
        assertEquals(Java8Vs17.leadingSmallOld(List.of(1, 2, 5, 1), 3), Java8Vs17.leadingSmallNew(List.of(1, 2, 5, 1), 3));
        assertEquals(List.of(1, 2), Java8Vs17.leadingSmallNew(List.of(1, 2, 5, 1), 3));
        assertEquals(0, Java8Vs17.ofNullableOld(null).count());
        assertEquals(0, Java8Vs17.ofNullableNew(null).count());
    }

    @Test
    void stringsAndRecords() {
        String text = "a\n\n  b  \n";
        assertEquals(Java8Vs17.nonBlankLinesOld(text), Java8Vs17.nonBlankLinesNew(text));
        assertEquals("ababab", Java8Vs17.repeatNew("ab", 3));
        assertEquals(Java8Vs17.repeatOld("ab", 3), Java8Vs17.repeatNew("ab", 3));
        assertEquals(Java8Vs17.joinOld(List.of("a", "b")), Java8Vs17.joinNew(List.of("a", "b")));
        assertEquals(new Java8Vs17.PointOld(1, 2), new Java8Vs17.PointOld(1, 2));
        assertEquals(new Java8Vs17.PointNew(1, 2), new Java8Vs17.PointNew(1, 2));
        assertEquals("PointNew[x=1, y=2]", new Java8Vs17.PointNew(1, 2).toString());
    }
}
