package module03.lists;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListDemosTest {

    private static List<String> sample() {
        return new ArrayList<>(List.of("a-ok", "b-declined", "c-ok", "d-declined", "e-declined"));
    }

    @Test
    void forEachRemoveThrowsCme() {
        assertThrows(ConcurrentModificationException.class, () -> ListDemos.removeDeclinedBad(sample()));
    }

    @Test
    void iteratorRemoveAndRemoveIfWork() {
        List<String> a = sample();
        ListDemos.removeDeclinedGood(a);
        assertEquals(List.of("a-ok", "c-ok"), a);
        List<String> b = sample();
        ListDemos.removeDeclinedBest(b);
        assertEquals(List.of("a-ok", "c-ok"), b);
    }

    @Test
    void bothListTypesHoldSameContent() {
        assertEquals(ListDemos.buildIds(new ArrayList<>(), 100), ListDemos.buildIds(new LinkedList<>(), 100));
        assertEquals(0, ListDemos.presized(1000).size());
    }
}
