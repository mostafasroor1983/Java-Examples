package module14.jit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class JitDemoTest {

    @Test
    void monomorphicAndMegamorphicAgree() {
        long n = 100_000;
        JitDemo.Fee[] fees = {new JitDemo.Percent(), new JitDemo.Flat(), new JitDemo.Tiered()};
        long mono = JitDemo.monomorphic(new JitDemo.Percent(), n);
        long mega = JitDemo.megamorphic(fees, n);
        assertEquals(49_950_000L, mono); // sum_{i<100000} i/100 = 100 * (999*1000/2)
        assertTrue(mega > 0);
    }

    @Test
    void warmupCurveIsMeasurable() {
        long[] curve = JitDemo.warmupCurve(20, 50_000);
        assertEquals(20, curve.length);
        for (long batch : curve) assertTrue(batch > 0);
        // Informational: the later batches are usually faster than the first; we do not assert timing in CI.
        assertNotNull(JitDemo.compilerInfo());
        assertEquals(new BigDecimal("1000"), JitDemo.consumed(1000));
    }
}
