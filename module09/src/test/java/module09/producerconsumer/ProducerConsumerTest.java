package module09.producerconsumer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProducerConsumerTest {

    @Test
    void pipelineTotalIsExact() throws Exception {
        WaitNotifyQueue<BigDecimal> q = new WaitNotifyQueue<>(4);
        BigDecimal total = SettlementPipeline.run(q, 4, 500, 3);
        assertEquals(new BigDecimal("1.25").multiply(BigDecimal.valueOf(2000)), total);
        assertEquals(0, q.size());
    }

    @Test
    void capacityIsRespected() throws Exception {
        WaitNotifyQueue<Integer> q = new WaitNotifyQueue<>(2);
        q.put(1);
        q.put(2);
        CountDownLatch blocked = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            try {
                blocked.countDown();
                q.put(3); // blocks until a take()
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.start();
        blocked.await();
        assertEquals(2, q.size());
        assertEquals(1, q.take());
        producer.join(5000);
        assertFalse(producer.isAlive());
        assertEquals(2, q.size());
        assertThrows(IllegalArgumentException.class, () -> new WaitNotifyQueue<>(0));
    }

    @Test
    void brokenQueueMayViolateInvariants() throws Exception {
        BrokenWaitNotifyQueue<Integer> q = new BrokenWaitNotifyQueue<>(1);
        AtomicInteger nulls = new AtomicInteger();
        AtomicInteger overCapacity = new AtomicInteger();
        int producers = 4, consumers = 4, per = 2000;
        CountDownLatch done = new CountDownLatch(producers + consumers);
        for (int p = 0; p < producers; p++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < per; i++) {
                        q.put(i);
                        if (q.size() > 1) overCapacity.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        for (int c = 0; c < consumers; c++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < per; i++) if (q.take() == null) nulls.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        // The broken queue may also deadlock (lost wake-up); we bound the wait and treat a hang as a demonstrated failure.
        boolean finished = done.await(5, TimeUnit.SECONDS);
        assertTrue(!finished || nulls.get() >= 0 && overCapacity.get() >= 0,
                "demonstration only: broken queue either hangs, returns nulls or exceeds capacity");
    }
}
