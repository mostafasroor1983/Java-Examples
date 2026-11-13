package module05.methods;

import module05.bounds.Money;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectionUtilsTest {

    @Test
    void filterMapGroup() {
        List<Integer> nums = List.of(1, 2, 3, 4);
        assertEquals(List.of(2, 4), CollectionUtils.filter(nums, n -> n % 2 == 0));
        assertEquals(List.of("1", "2", "3", "4"), CollectionUtils.map(nums, String::valueOf));
        Map<Boolean, List<Integer>> g = CollectionUtils.groupBy(nums, n -> n > 2);
        assertEquals(List.of(3, 4), g.get(true));
    }

    @Test
    void maxWithNaturalAndCustomOrder() {
        List<Money> m = List.of(Money.of("1", "GBP"), Money.of("3", "GBP"), Money.of("2", "GBP"));
        assertEquals(Money.of("3", "GBP"), CollectionUtils.max(m));
        assertEquals(Money.of("1", "GBP"), CollectionUtils.maxBy(m, Comparator.reverseOrder()));
        assertThrows(IllegalArgumentException.class, () -> CollectionUtils.max(List.<Integer>of()));
    }

    @Test
    void contravariantPredicateAccepted() {
        java.util.function.Predicate<Object> notNull = o -> o != null;
        assertEquals(List.of("a"), CollectionUtils.filter(List.of("a"), notNull));
    }
}
