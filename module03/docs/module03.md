# Module 03 - Collections and HashMap Internals

Package root: `module03`. Domain: transaction lists, settlement queues, fee tiers, FX pairs, account caches, ledgers.

## Choosing a collection (the 10-second answer)
| Need | Use | Why |
|---|---|---|
| Indexed list, iteration | `ArrayList` | contiguous memory, O(1) get, cache friendly |
| Stack / queue / deque | `ArrayDeque` | faster than `LinkedList`, never use `Stack` |
| Ordering by priority | `PriorityQueue` | binary heap, O(log n) offer/poll |
| Key lookup | `HashMap` | O(1) average |
| Sorted keys, range queries | `TreeMap` | red-black tree, O(log n), `floorEntry`, `subMap` |
| Insertion/access order | `LinkedHashMap` | LRU in 10 lines |
| Set semantics | `HashSet` / `TreeSet` | wrappers around the maps |

## HashMap internals (`module03.hashmapinternals`)
1. `h = key.hashCode()`
2. spread: `h ^ (h >>> 16)` so high bits influence the index
3. `index = (n - 1) & h` with `n` a power of two
4. bucket is a linked list; **treeified** to a red-black tree when the bucket reaches 8 nodes and the table has at least 64 slots (`TREEIFY_THRESHOLD`, `MIN_TREEIFY_CAPACITY`); untreeified below 6
5. resize at `size > capacity * 0.75`; the table doubles and each bucket splits in two (lo/hi lists) without rehashing

`ColliderKey` has a constant `hashCode`; the test inserts 100 of them to force treeify and shows lookups still work.
Because it implements `Comparable`, tree lookups are O(log n); without it the tree degrades to tie-break by identity.

`HashMapInternals.capacityFor(expected)` shows why `new HashMap<>(expected)` is **wrong** when you want no resize:
you need `expected / 0.75` rounded up to a power of two.

## BAD vs GOOD
```java
// BAD: ConcurrentModificationException
for (String tx : txs) if (tx.endsWith("-declined")) txs.remove(tx);
// GOOD
txs.removeIf(tx -> tx.endsWith("-declined"));
```
```java
// BAD: three map operations, racy on a concurrent map
Integer c = counts.get(k); counts.put(k, c == null ? 1 : c + 1);
// GOOD
counts.merge(k, 1, Integer::sum);
```

## Exercises
1. **FxPairMap** - composite record key `Pair(base, quote)`; direct, inverse (`1/r`) and cross-rate via USD.
2. **EventFrequencyAnalyzer** - three versions of counting (`get/put`, `getOrDefault`, `merge`), deterministic top-N, `TreeMap` and `LinkedHashMap` views.
3. **AccountCache v1/v2/v3** - unbounded `HashMap`; `LinkedHashMap(accessOrder=true)` + `removeEldestEntry`; hand-rolled `HashMap` + doubly linked list with O(1) operations.

`module03.ledgers.TransactionLedger` combines all three structures and exposes a reconciliation invariant.

## Key Takeaways
- Default to `ArrayList` and `ArrayDeque`; `LinkedList` almost never wins.
- `HashMap` iteration order is unspecified and changes after resize: never depend on it.
- `TreeMap.floorEntry` is the idiomatic tool for tiered pricing and "balance as of".
- `LinkedHashMap` in access-order mode is a complete LRU cache.
- Pre-size maps using `expected / loadFactor`, not `expected`.
- Bad `hashCode` turns O(1) into O(n) (or O(log n) after treeify, if `Comparable`).

## Cheat Sheet
| Operation | ArrayList | LinkedList | ArrayDeque | HashMap | TreeMap |
|---|---|---|---|---|---|
| get(i) | O(1) | O(n) | - | - | - |
| add at end | O(1)* | O(1) | O(1)* | - | - |
| add at head | O(n) | O(1) | O(1)* | - | - |
| contains/get key | O(n) | O(n) | O(n) | O(1)* | O(log n) |
| range query | - | - | - | - | O(log n + k) |
`*` amortised / average.

## 10 Interview Q&A
1. **How does HashMap handle collisions?** Linked list per bucket, becomes a red-black tree at 8 nodes when the table has 64+ slots.
2. **Why is the table size a power of two?** `(n-1) & hash` replaces modulo; resize can split buckets with one bit test.
3. **What does the load factor control?** Memory vs collision trade-off; 0.75 is the default balance.
4. **Is HashMap ordered?** No. `LinkedHashMap` keeps insertion/access order, `TreeMap` keeps sorted order.
5. **What is the complexity of `TreeMap.get`?** O(log n) with comparisons; keys must be `Comparable` or you supply a `Comparator`.
6. **How would you implement an LRU cache?** `LinkedHashMap(cap, 0.75f, true)` + `removeEldestEntry`, or HashMap + doubly linked list for full control.
7. **Why is `ArrayList.remove(int)` O(n)?** It shifts the tail left with `System.arraycopy`.
8. **What is fail-fast iteration?** `modCount` check throws `ConcurrentModificationException` on structural change during iteration; it is best effort, not a guarantee.
9. **`PriorityQueue` iteration order?** Heap order, not sorted. Only `poll()` gives sorted output.
10. **When would you pick `LinkedList`?** Almost never; `ArrayDeque` for deque ops, `ArrayList` otherwise. Possibly when you hold a `ListIterator` and remove in the middle repeatedly.

## Exercises for the reader
1. Add a `TreeMap`-based `between(Instant, Instant)` to `AccountCacheV3` tracking last access time (LRU with TTL).
2. Measure `ArrayList` vs `LinkedList` for 1M `add(0, x)` calls and explain the result.
3. Write a `MultiCurrencyLedger` keyed by `(accountId, currency)` record and reconcile per currency.
