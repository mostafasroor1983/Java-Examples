package module10.memory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Stack: per-thread frames holding locals and references; freed on return; StackOverflowError on deep recursion.
 * Heap: objects shared by all threads, managed by GC; OutOfMemoryError when exhausted.
 * Escape analysis may scalar-replace short-lived objects (allocating "on the stack" conceptually).
 */
public final class HeapVsStack {
    private HeapVsStack() {
    }

    /** Locals live on the stack frame; each thread gets its own copy - no sharing, no race. */
    public static long sumLocals(int n) {
        long acc = 0;           // stack
        for (int i = 0; i < n; i++) acc += i;   // stack
        return acc;
    }

    /** Depth at which recursion overflows. Illustrates frame size vs default stack (usually 512KB-1MB). */
    public static int recursionDepthUntilOverflow() {
        int[] depth = {0};
        try {
            recurse(depth);
        } catch (StackOverflowError expected) {
            return depth[0];
        }
        return -1;
    }

    private static void recurse(int[] depth) {
        depth[0]++;
        recurse(depth);
    }

    /** Objects live on the heap and are shared by reference; two threads touching this list need synchronisation. */
    public static List<BigDecimal> allocateOnHeap(int n) {
        List<BigDecimal> list = new ArrayList<>(n); // the reference is on the stack, the ArrayList and BigDecimals on the heap
        for (int i = 0; i < n; i++) list.add(BigDecimal.valueOf(i));
        return list;
    }

    /** Pass-by-value of references: reassigning the parameter does not affect the caller; mutating the object does. */
    public static void reassignParameter(List<String> param) {
        param = new ArrayList<>(); // caller's reference unchanged
        param.add("lost");
    }

    public static void mutateParameter(List<String> param) {
        param.add("visible"); // caller sees it
    }

    /** Primitive vs boxed: `int` lives inline; `Integer` is a heap object (with a -128..127 cache). */
    public static boolean boxedIdentityTrap(int a, int b) {
        Integer x = a;
        Integer y = b;
        return x == y; // reference comparison! true only within the cache range
    }

    public static boolean boxedEqualsCorrect(int a, int b) {
        Integer x = a;
        Integer y = b;
        return x.equals(y);
    }

    /** Memory stats are approximate but show heap growth after allocation. */
    public static long usedHeapBytes() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }
}
