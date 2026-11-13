package module04.queues;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Blocking vs non-blocking queues.
 * - ArrayBlockingQueue: bounded, producers block when full -> natural back-pressure.
 * - ConcurrentLinkedQueue: unbounded, lock-free, never blocks -> fast but no back-pressure; can OOM.
 * The pipeline uses a poison pill to stop consumers deterministically.
 */
public final class PaymentEventPipeline {
    public static final String POISON = "__STOP__";

    private PaymentEventPipeline() {
    }

    /** Runs `producers` producer threads each emitting `perProducer` events through a bounded queue to `consumers` consumer threads. */
    public static List<String> runBounded(int capacity, int producers, int perProducer, int consumers) throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(capacity);
        List<String> consumed = new ArrayList<>();
        CountDownLatch producersDone = new CountDownLatch(producers);
        CountDownLatch consumersDone = new CountDownLatch(consumers);

        for (int p = 0; p < producers; p++) {
            int id = p;
            Thread.ofPlatform().start(() -> {
                try {
                    for (int i = 0; i < perProducer; i++) {
                        queue.put("p" + id + "-e" + i); // blocks when full
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    producersDone.countDown();
                }
            });
        }
        for (int c = 0; c < consumers; c++) {
            Thread.ofPlatform().start(() -> {
                try {
                    while (true) {
                        String ev = queue.take();
                        if (POISON.equals(ev)) break;
                        synchronized (consumed) {
                            consumed.add(ev);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    consumersDone.countDown();
                }
            });
        }
        producersDone.await();
        for (int c = 0; c < consumers; c++) queue.put(POISON);
        if (!consumersDone.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("consumers did not finish");
        }
        return consumed;
    }

    /** Offer with timeout: the caller decides what to do on back-pressure instead of blocking forever. */
    public static boolean tryEnqueue(BlockingQueue<String> queue, String event, long timeoutMs) throws InterruptedException {
        return queue.offer(Objects.requireNonNull(event), timeoutMs, TimeUnit.MILLISECONDS);
    }

    /** Non-blocking queue: offer always succeeds; poll returns null when empty. */
    public static ConcurrentLinkedQueue<String> unbounded() {
        return new ConcurrentLinkedQueue<>();
    }
}
