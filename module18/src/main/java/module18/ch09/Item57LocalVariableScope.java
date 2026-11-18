package module18.ch09;

import java.util.Iterator;
import java.util.List;

/**
 * Item 57 - Minimize the scope of local variables.
 *
 * <p>Declare a variable where it is first used and initialise it there. Prefer {@code for} loops to {@code while}
 * loops: the loop variable's scope ends with the loop, which prevents the copy-paste bug where a second loop
 * accidentally reuses the FIRST iterator ({@link #copyPasteBug}). Keep methods small and focused.
 */
public final class Item57LocalVariableScope {
    private Item57LocalVariableScope() {
    }

    /** BAD: i2's loop tests the stale iterator i, so the second list is silently skipped. */
    public static int copyPasteBug(List<String> pending, List<String> failed) {
        int processed = 0;
        Iterator<String> i = pending.iterator();
        while (i.hasNext()) {
            i.next();
            processed++;
        }
        Iterator<String> i2 = failed.iterator();
        while (i.hasNext()) { // BUG: should be i2 - compiles fine, processes nothing
            i2.next();
            processed++;
        }
        return processed;
    }

    /** GOOD: each iterator/variable is scoped to its loop; the same bug would not compile. */
    public static int processAll(List<String> pending, List<String> failed) {
        int processed = 0;
        for (Iterator<String> i = pending.iterator(); i.hasNext(); ) {
            i.next();
            processed++;
        }
        for (String ignored : failed) processed++;
        return processed;
    }
}
