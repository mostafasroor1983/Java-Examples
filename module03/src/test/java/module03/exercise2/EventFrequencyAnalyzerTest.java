package module03.exercise2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventFrequencyAnalyzerTest {

    @Test
    void allThreeRecordVariantsCount() {
        EventFrequencyAnalyzer a = new EventFrequencyAnalyzer();
        a.recordV1("AUTH");
        a.recordV2("AUTH");
        a.record("AUTH");
        a.record("DECLINE");
        assertEquals(3, a.count("AUTH"));
        assertEquals(1, a.count("DECLINE"));
        assertEquals(0, a.count("REFUND"));
    }

    @Test
    void topNIsDeterministic() {
        EventFrequencyAnalyzer a = new EventFrequencyAnalyzer();
        for (String e : List.of("B", "A", "A", "C", "B", "D")) a.record(e);
        var top = a.topN(3);
        assertEquals(List.of("A", "B", "C"), top.stream().map(Map.Entry::getKey).toList());
        assertEquals(4, a.topN(10).size());
        assertThrows(IllegalArgumentException.class, () -> a.topN(-1));
        assertEquals("A", a.sortedByName().firstKey());
    }

    @Test
    void linkedHashMapKeepsFirstSeenOrder() {
        var m = EventFrequencyAnalyzer.firstSeenOrder(List.of("Z", "A", "Z", "M"));
        assertEquals(List.of("Z", "A", "M"), List.copyOf(m.keySet()));
        assertEquals(2, m.get("Z"));
    }
}
