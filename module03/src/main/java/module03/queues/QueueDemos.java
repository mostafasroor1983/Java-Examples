package module03.queues;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.PriorityQueue;

/** ArrayDeque for stacks/queues (never java.util.Stack), PriorityQueue for ordering by priority. */
public final class QueueDemos {
    private QueueDemos() {
    }

    public record SettlementJob(String id, int priority, BigDecimal amount) {
    }

    /** Undo stack of ledger operations: push/pop at the head. */
    public static Deque<String> undoStack() {
        return new ArrayDeque<>();
    }

    /** FIFO queue of incoming payment events. */
    public static Deque<String> fifo() {
        return new ArrayDeque<>();
    }

    /** Highest priority first; ties broken by larger amount first. PriorityQueue is a binary heap: O(log n) add/poll. */
    public static PriorityQueue<SettlementJob> settlementQueue() {
        return new PriorityQueue<>(Comparator.comparingInt(SettlementJob::priority).reversed()
                .thenComparing(SettlementJob::amount, Comparator.reverseOrder()));
    }

    /** Checks whether brackets in a text (e.g. a rule expression) are balanced - classic stack problem. */
    public static boolean balanced(String expr) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : expr.toCharArray()) {
            switch (c) {
                case '(', '[', '{' -> stack.push(c);
                case ')' -> { if (stack.isEmpty() || stack.pop() != '(') return false; }
                case ']' -> { if (stack.isEmpty() || stack.pop() != '[') return false; }
                case '}' -> { if (stack.isEmpty() || stack.pop() != '{') return false; }
                default -> { }
            }
        }
        return stack.isEmpty();
    }
}
