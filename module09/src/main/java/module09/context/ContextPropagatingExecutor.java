package module09.context;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Decorator that propagates {@link CorrelationContext} from the submitting thread to the worker thread and
 * ALWAYS restores the worker's previous state afterwards, so nothing leaks into the next task on a pooled thread.
 */
public final class ContextPropagatingExecutor implements Executor {
    private final Executor delegate;

    public ContextPropagatingExecutor(Executor delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void execute(Runnable task) {
        String captured = CorrelationContext.current().orElse(null); // capture on the CALLER thread
        delegate.execute(() -> {
            String previous = CorrelationContext.current().orElse(null);
            if (captured == null) CorrelationContext.clear(); else CorrelationContext.set(captured);
            try {
                task.run();
            } finally {
                if (previous == null) CorrelationContext.clear(); else CorrelationContext.set(previous);
            }
        });
    }
}
