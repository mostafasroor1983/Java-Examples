package module04.queues;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

class PaymentEventPipelineTest {

    @Test
    void boundedPipelineDeliversEveryEventExactlyOnce() throws Exception {
        List<String> consumed = PaymentEventPipeline.runBounded(8, 4, 500, 3);
        assertEquals(2000, consumed.size());
        assertEquals(2000, new HashSet<>(consumed).size());
    }

    @Test
    void offerWithTimeoutReportsBackPressure() throws Exception {
        BlockingQueue<String> q = new ArrayBlockingQueue<>(1);
        assertTrue(PaymentEventPipeline.tryEnqueue(q, "e1", 10));
        assertFalse(PaymentEventPipeline.tryEnqueue(q, "e2", 10), "queue full, offer times out");
        assertEquals("e1", q.poll());
    }

    @Test
    void unboundedQueueNeverBlocks() {
        ConcurrentLinkedQueue<String> q = PaymentEventPipeline.unbounded();
        for (int i = 0; i < 10_000; i++) assertTrue(q.offer("e" + i));
        assertEquals("e0", q.poll());
        assertEquals(9_999, q.size());
    }
}
