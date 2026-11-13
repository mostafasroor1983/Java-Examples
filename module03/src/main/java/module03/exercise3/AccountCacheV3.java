package module03.exercise3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * v3: hand-rolled LRU with HashMap + doubly linked list. O(1) get/put, no reliance on
 * LinkedHashMap internals, and the structure you would extend with TTL or size-based weights.
 */
public class AccountCacheV3 {
    private static final class Node {
        final String key;
        AccountCache.Account value;
        Node prev, next;

        Node(String key, AccountCache.Account value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<String, Node> index = new HashMap<>();
    private final Node head = new Node(null, null); // most recent after head
    private final Node tail = new Node(null, null); // least recent before tail

    public AccountCacheV3(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public synchronized Optional<AccountCache.Account> get(String id) {
        Node n = index.get(id);
        if (n == null) return Optional.empty();
        moveToFront(n);
        return Optional.of(n.value);
    }

    public synchronized void put(AccountCache.Account account) {
        Node n = index.get(account.id());
        if (n != null) {
            n.value = account;
            moveToFront(n);
            return;
        }
        n = new Node(account.id(), account);
        index.put(account.id(), n);
        addFront(n);
        if (index.size() > capacity) {
            Node lru = tail.prev;
            unlink(lru);
            index.remove(lru.key);
        }
    }

    public synchronized int size() {
        return index.size();
    }

    private void moveToFront(Node n) {
        unlink(n);
        addFront(n);
    }

    private void addFront(Node n) {
        n.next = head.next;
        n.prev = head;
        head.next.prev = n;
        head.next = n;
    }

    private void unlink(Node n) {
        n.prev.next = n.next;
        n.next.prev = n.prev;
    }
}
