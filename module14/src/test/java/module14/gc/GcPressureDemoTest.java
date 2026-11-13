package module14.gc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GcPressureDemoTest {

    @Test
    void heavyAndLightSumsAgree() {
        assertEquals(GcPressureDemo.sumAllocHeavy(10_000), GcPressureDemo.sumAllocLight(10_000));
        assertEquals(new BigDecimal("499950.00"), GcPressureDemo.sumAllocLight(10_000));
    }

    @Test
    void retainedVsTransientAndCollectorInfo() {
        assertEquals(1000, GcPressureDemo.retainedVsTransient(1000, true));
        assertTrue(GcPressureDemo.retainedVsTransient(1000, false) != Integer.MIN_VALUE);
        assertFalse(GcPressureDemo.collectorNames().isEmpty());
        assertTrue(GcPressureDemo.totalGcCount() >= 0);
    }

    @Test
    void reusableBufferIsCorrectAcrossCalls() {
        var buf = new GcPressureDemo.ReusableBuffer();
        assertEquals("p1|10.50", buf.format("p1", new BigDecimal("10.50")));
        assertEquals("p2|0.01", buf.format("p2", new BigDecimal("0.01")), "setLength(0) resets the shared builder");
    }
}
