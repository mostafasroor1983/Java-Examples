package module02.records;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRecordTest {

    @Test
    void recordEqualityIsStructural() {
        assertEquals(Payment.of("p1", "10.00", "GBP"), Payment.of("p1", "10.00", "GBP"));
        assertEquals(Payment.of("p1", "10.00", "GBP").hashCode(), Payment.of("p1", "10.00", "GBP").hashCode());
        assertNotEquals(Payment.of("p1", "10.0", "GBP"), Payment.of("p1", "10.00", "GBP"),
                "BigDecimal component => scale matters in records unless normalised");
    }

    @Test
    void compactConstructorValidatesAndCopies() {
        List<String> tags = new ArrayList<>(List.of("a"));
        Payment p = new Payment("p1", BigDecimal.ONE, "GBP", tags);
        tags.add("b");

        // Correct, because p.tags() is copy not able to modify
        assertEquals(List.of("a"), p.tags());
        assertThrows(UnsupportedOperationException.class, () -> p.tags().add("c"));
        assertThrows(IllegalArgumentException.class, () -> Payment.of("p", "0", "GBP"));
        assertEquals(List.of("a", "x"), p.withTag("x").tags());
    }

    @Test
    void leakyRecordCanBeMutatedFromOutside() {
        List<String> tags = new ArrayList<>(List.of("a"));
        LeakyPayment leaky = new LeakyPayment("p", tags);
        tags.add("b");
        assertEquals(2, leaky.tags().size(), "record is only shallowly immutable");
    }
}
