package module03.queues;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Deque;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

class QueueDemosTest {

    @Test
    void stackIsLifoQueueIsFifo() {
        Deque<String> stack = QueueDemos.undoStack();
        stack.push("op1");
        stack.push("op2");
        assertEquals("op2", stack.pop());
        Deque<String> q = QueueDemos.fifo();
        q.offer("e1");
        q.offer("e2");
        assertEquals("e1", q.poll());
    }

    @Test
    void priorityQueueOrdersByPriorityThenAmount() {
        PriorityQueue<QueueDemos.SettlementJob> pq = QueueDemos.settlementQueue();
        pq.add(new QueueDemos.SettlementJob("low", 1, BigDecimal.TEN));
        pq.add(new QueueDemos.SettlementJob("high-small", 5, BigDecimal.ONE));
        pq.add(new QueueDemos.SettlementJob("high-big", 5, BigDecimal.TEN));
        assertEquals("high-big", pq.poll().id());
        assertEquals("high-small", pq.poll().id());
        assertEquals("low", pq.poll().id());
        assertNull(pq.poll());
    }

    @Test
    void balancedBrackets() {
        assertTrue(QueueDemos.balanced("(amount > 10 && [tier == {PREMIUM}])"));
        assertFalse(QueueDemos.balanced("(]"));
        assertFalse(QueueDemos.balanced("(("));
        assertFalse(QueueDemos.balanced(")"));
        assertTrue(QueueDemos.balanced(""));
    }
}
