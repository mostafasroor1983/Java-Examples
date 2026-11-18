package module09.context;

import java.util.Optional;

/**
 * Per-request correlation id held in a ThreadLocal, the same mechanism behind SLF4J's MDC and many tracing libraries.
 *
 * <p>Two classic bugs with thread pools:
 * <ol>
 *   <li><b>Lost context</b>: work submitted to an executor runs on a pool thread whose ThreadLocal is empty, so logs
 *   for half the payment flow have no correlation id and an incident cannot be traced end to end.</li>
 *   <li><b>Leaked context</b>: a pool thread that set the id and never cleared it carries the PREVIOUS request's id
 *   into the next task, so customer A's logs show up under customer B's trace. That is a privacy and audit problem.</li>
 * </ol>
 * Fix: {@link ContextPropagatingExecutor} captures at submit time and restores+clears around each task.
 * Java 21 offers ScopedValue (module13), which is immutable and bounded to a scope, so neither bug can happen.
 */
public final class CorrelationContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationContext() {
    }

    public static Optional<String> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void set(String correlationId) {
        CURRENT.set(correlationId);
    }

    public static void clear() {
        CURRENT.remove(); // remove(), not set(null): avoids keeping the entry in the thread's map
    }
}
