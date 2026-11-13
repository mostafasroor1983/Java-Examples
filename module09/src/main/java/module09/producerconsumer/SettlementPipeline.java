package module09.producerconsumer;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Producers publish settlement amounts; consumers sum them. Exact total proves no item is lost or duplicated. */
public final class SettlementPipeline {
    private SettlementPipeline() {
    }

    private static final BigDecimal POISON = new BigDecimal("-1");

    public static BigDecimal run(WaitNotifyQueue<BigDecimal> queue, int producers, int perProducer, int consumers) throws InterruptedException {
        AtomicReference<BigDecimal> total = new AtomicReference<>(BigDecimal.ZERO);
        CountDownLatch producersDone = new CountDownLatch(producers);
        CountDownLatch consumersDone = new CountDownLatch(consumers);

        for (int p = 0; p < producers; p++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < perProducer; i++) queue.put(new BigDecimal("1.25"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    producersDone.countDown();
                }
            }, "producer-" + p).start();
        }
        for (int c = 0; c < consumers; c++) {
            new Thread(() -> {
                try {
                    while (true) {
                        BigDecimal v = queue.take();
                        if (v == POISON) break;
                        total.accumulateAndGet(v, BigDecimal::add);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    consumersDone.countDown();
                }
            }, "consumer-" + c).start();
        }
        producersDone.await();
        for (int c = 0; c < consumers; c++) queue.put(POISON);
        if (!consumersDone.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("consumers stuck");
        return total.get();
    }
}
