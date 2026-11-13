package module15.p11batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * P11 - Batch processor: buffers items and flushes when size reaches `batchSize` or when `flush()` is called
 * (a scheduler would call it on a timer). Flush of a failing batch keeps the items for retry. Thread-safe.
 */
public final class P11BatchProcessor<T> {
    private final int batchSize;
    private final Consumer<List<T>> sink;
    private final List<T> buffer = new ArrayList<>();
    private int batchesFlushed;

    public P11BatchProcessor(int batchSize, Consumer<List<T>> sink) {
        if (batchSize <= 0) throw new IllegalArgumentException("batchSize must be positive");
        this.batchSize = batchSize;
        this.sink = Objects.requireNonNull(sink);
    }

    public void add(T item) {
        List<T> toFlush = null;
        synchronized (buffer) {
            buffer.add(Objects.requireNonNull(item));
            if (buffer.size() >= batchSize) toFlush = drain();
        }
        if (toFlush != null) deliver(toFlush); // deliver outside the lock
    }

    public void flush() {
        List<T> toFlush;
        synchronized (buffer) {
            if (buffer.isEmpty()) return;
            toFlush = drain();
        }
        deliver(toFlush);
    }

    private List<T> drain() {
        List<T> copy = new ArrayList<>(buffer);
        buffer.clear();
        return copy;
    }

    private void deliver(List<T> batch) {
        try {
            sink.accept(List.copyOf(batch));
            synchronized (buffer) {
                batchesFlushed++;
            }
        } catch (RuntimeException e) {
            synchronized (buffer) {
                buffer.addAll(0, batch); // put back at the front to preserve order
            }
            throw e;
        }
    }

    public int pending() {
        synchronized (buffer) {
            return buffer.size();
        }
    }

    public int batchesFlushed() {
        synchronized (buffer) {
            return batchesFlushed;
        }
    }
}
