package module15.p08lru;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * P08 - LRU cache with O(1) get/put: HashMap + doubly linked list. Thread-safe via a single lock
 * (fine for an interview; production would use Caffeine). Tracks hits/misses.
 */
public final class P08LruCache<K, V> {
    private final class Node {
        final K key;
        V value;
        Node prev, next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<K, Node> map = new HashMap<>();
    private final Node head = new Node(null, null), tail = new Node(null, null);
    private long hits, misses;

    public P08LruCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public synchronized Optional<V> get(K key) {
        Node n = map.get(key);
        if (n == null) {
            misses++;
            return Optional.empty();
        }
        hits++;
        moveToFront(n);
        return Optional.of(n.value);
    }

    public synchronized void put(K key, V value) {
        Node n = map.get(key);
        if (n != null) {
            n.value = value;
            moveToFront(n);
            return;
        }
        if (map.size() == capacity) {
            Node lru = tail.prev;
            unlink(lru);
            map.remove(lru.key);
        }
        n = new Node(key, value);
        map.put(key, n);
        addFront(n);
    }

    public synchronized boolean remove(K key) {
        Node n = map.remove(key);
        if (n == null) return false;
        unlink(n);
        return true;
    }

    public synchronized int size() {
        return map.size();
    }

    public synchronized long hits() {
        return hits;
    }

    public synchronized long misses() {
        return misses;
    }

    /** Keys from most to least recently used - handy for tests. */
    public synchronized java.util.List<K> keysMruFirst() {
        java.util.List<K> out = new java.util.ArrayList<>();
        for (Node n = head.next; n != tail; n = n.next) out.add(n.key);
        return out;
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
