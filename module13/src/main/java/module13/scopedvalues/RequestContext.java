package module13.scopedvalues;

import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;

/**
 * ScopedValue (preview in 21): an immutable value bound for the dynamic extent of a call and automatically
 * inherited by subtasks forked in a StructuredTaskScope. Replaces ThreadLocal for request metadata
 * (trace id, tenant, authenticated user) - no leaks, no manual propagation, no remove().
 */
public final class RequestContext {
    public record Context(String traceId, String customerId) {
    }

    public static final ScopedValue<Context> CONTEXT = ScopedValue.newInstance();

    private RequestContext() {
    }

    public static <T> T runWith(Context ctx, Callable<T> body) throws Exception {
        return ScopedValue.callWhere(CONTEXT, ctx, body);
    }

    public static Context current() {
        return CONTEXT.get(); // NoSuchElementException if unbound
    }

    public static boolean isBound() {
        return CONTEXT.isBound();
    }

    /** Rebinding in a nested scope shadows the outer value only inside the nested call. */
    public static String nestedRebinding() throws Exception {
        return runWith(new Context("outer", "c1"), () -> {
            String outer = current().traceId();
            String inner = runWith(new Context("inner", "c1"), () -> current().traceId());
            return outer + "/" + inner + "/" + current().traceId();
        });
    }

    /** Subtasks forked inside the scope see the same binding without any plumbing. */
    public static String traceIdSeenBySubtasks() throws Exception {
        return runWith(new Context("trace-42", "c9"), () -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                var a = scope.fork(() -> current().traceId());
                var b = scope.fork(() -> Thread.currentThread().isVirtual() + ":" + current().traceId());
                scope.join().throwIfFailed();
                return a.get() + "|" + b.get();
            }
        });
    }

    /** The ThreadLocal way, for contrast: mutable, must be removed, not inherited by pooled threads. */
    public static final class ThreadLocalContext {
        private static final ThreadLocal<Context> TL = new ThreadLocal<>();

        public static <T> T runWith(Context ctx, Callable<T> body) throws Exception {
            TL.set(ctx);
            try {
                return body.call();
            } finally {
                TL.remove(); // forgetting this leaks the context to the next request on a pooled thread
            }
        }

        public static Context current() {
            return TL.get();
        }
    }
}
