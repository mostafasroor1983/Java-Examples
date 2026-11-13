package module15.p09producerconsumer;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class P09Test {
    @Test
    void timedOperations() throws Exception {
        var b = new P09BoundedBuffer<Integer>(1);
        assertTrue(b.offer(1, 10, TimeUnit.MILLISECONDS));
        assertFalse(b.offer(2, 10, TimeUnit.MILLISECONDS));
        assertEquals(1, b.poll(10, TimeUnit.MILLISECONDS));
        assertNull(b.poll(10, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> new P09BoundedBuffer<>(0));
    }

    @Test
    void exactSumAcrossProducersAndConsumers() throws Exception {
        var b = new P09BoundedBuffer<Integer>(8);
        int producers = 4, consumers = 3, per = 1000;
        AtomicLong sum = new AtomicLong();
        CountDownLatch producersDone = new CountDownLatch(producers), consumersDone = new CountDownLatch(consumers);
        for (int p = 0; p < producers; p++) new Thread(() -> { try { for (int i = 1; i <= per; i++) b.put(i); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } finally { producersDone.countDown(); } }).start();
        for (int c = 0; c < consumers; c++) new Thread(() -> { try { while (true) { Integer v = b.take(); if (v < 0) break; sum.addAndGet(v); } } catch (InterruptedException e) { Thread.currentThread().interrupt(); } finally { consumersDone.countDown(); } }).start();
        assertTrue(producersDone.await(20, TimeUnit.SECONDS));
        for (int c = 0; c < consumers; c++) b.put(-1);
        assertTrue(consumersDone.await(20, TimeUnit.SECONDS));
        assertEquals((long) producers * per * (per + 1) / 2, sum.get());
        assertEquals(0, b.size());
    }
}
