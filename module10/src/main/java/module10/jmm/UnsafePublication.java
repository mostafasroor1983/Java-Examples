package module10.jmm;

import java.math.BigDecimal;

/**
 * BAD: a plain field shared between threads. The JMM allows the reader to see a stale value forever
 * (JIT may hoist the read out of the loop) or to see a partially constructed object.
 * This class exists to be contrasted with SafePublication; its behaviour is unspecified by design.
 */
public final class UnsafePublication {
    private FxRate current; // not volatile, not final, not guarded

    public record FxRate(String pair, BigDecimal rate) {
    }

    public void publish(FxRate rate) {
        current = rate;
    }

    public FxRate read() {
        return current;
    }
}
