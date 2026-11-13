package module14.gc;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * GC pressure: allocation rate drives young-GC frequency; long-lived garbage promotes and drives old-GC.
 * Reduce allocation in hot paths (reuse buffers, primitives, avoid boxing/varargs/streams in tight loops),
 * keep objects short-lived (die in the nursery) or truly long-lived (cache), avoid the middle.
 */
public final class GcPressureDemo {
    private GcPressureDemo() {
    }

    public static long totalGcCount() {
        long n = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) n += Math.max(0, gc.getCollectionCount());
        return n;
    }

    public static String collectorNames() {
        List<String> names = new ArrayList<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) names.add(gc.getName());
        return String.join(", ", names);
    }

    /** BAD: a new BigDecimal per cent plus boxing in a List<Long>; allocates megabytes for a simple sum. */
    public static BigDecimal sumAllocHeavy(int n) {
        List<BigDecimal> items = new ArrayList<>();
        for (int i = 0; i < n; i++) items.add(new BigDecimal(i).movePointLeft(2));
        BigDecimal acc = BigDecimal.ZERO;
        for (BigDecimal b : items) acc = acc.add(b);
        return acc;
    }

    /** GOOD: accumulate in a long (cents) and convert once. Exact and allocation-free in the loop. */
    public static BigDecimal sumAllocLight(int n) {
        long cents = 0;
        for (int i = 0; i < n; i++) cents += i;
        return BigDecimal.valueOf(cents, 2);
    }

    /** Short-lived garbage dies young; a retained list promotes to old gen. */
    public static int retainedVsTransient(int n, boolean retain) {
        List<byte[]> retained = retain ? new ArrayList<>() : null;
        int checksum = 0;
        for (int i = 0; i < n; i++) {
            byte[] buf = new byte[1024];
            buf[0] = (byte) i;
            checksum += buf[0];
            if (retained != null) retained.add(buf);
        }
        return retained == null ? checksum : retained.size();
    }

    /** Buffer reuse pattern (e.g. encoding ISO-8583 messages): one buffer per thread. */
    public static final class ReusableBuffer {
        private final ThreadLocal<StringBuilder> buffers = ThreadLocal.withInitial(() -> new StringBuilder(256));

        public String format(String id, BigDecimal amount) {
            StringBuilder sb = buffers.get();
            sb.setLength(0);
            return sb.append(id).append('|').append(amount.toPlainString()).toString();
        }
    }
}
