# Module 04 - Concurrent Collections

Package root: `module04`. Domain: balances, idempotency keys, payment event pipelines, fraud rules, rate limiting.

## The one rule
**A thread-safe collection makes each *call* atomic, not your *sequence* of calls.**
`map.get(k)` followed by `map.put(k, v+1)` is a race on `ConcurrentHashMap` exactly as on `HashMap`;
the only difference is that CHM will not corrupt its internal structure.

## BAD vs GOOD
```java
// BAD - module04.chm.BrokenBalanceStore: check-then-act on CHM
BigDecimal current = balances.get(id);
balances.put(id, current.add(amount));      // lost update
```
```java
// GOOD - module04.chm.BalanceStore
balances.merge(id, amount, BigDecimal::add);                 // atomic credit
balances.computeIfPresent(id, (k, bal) -> {                   // atomic debit with invariant
    if (bal.compareTo(amount) < 0) throw new InsufficientFundsException(...);
    return bal.subtract(amount);
});
balances.replace(id, expected, newValue);                     // CAS-style optimistic update
```

## What is in the module
| Package | Class | Lesson |
|---|---|---|
| `chm` | `BalanceStore` | `putIfAbsent`, `merge`, `computeIfPresent`, `replace(k,old,new)` retry loop; `values()` is weakly consistent |
| `chm` | `BrokenBalanceStore` | get+put loses updates (test shows total <= expected) |
| `idempotency` | `IdempotencyCache` | `compute` claims the key once; TTL + lazy/eager purge; 32 concurrent callers run the operation once |
| `queues` | `PaymentEventPipeline` | `ArrayBlockingQueue` back-pressure (`put` blocks, `offer(timeout)` reports), poison pill shutdown; `ConcurrentLinkedQueue` never blocks |
| `cow` | `FraudRuleRegistry` | `CopyOnWriteArrayList`: snapshot iteration, writes copy the array, read-mostly only |
| `races` | `LostUpdateDemo` | HashMap vs CHM get+put vs `merge` vs `LongAdder` |
| `ratelimiter` | `SlidingWindowRateLimiter` | per-key timestamp deque + CHM; exact limit under 16 threads |

## Rate limiter design notes
- **Fixed window** - cheap, but allows 2x burst at the boundary.
- **Sliding log** (this module) - exact, O(limit) memory per key, O(evictions) per call.
- **Sliding counter / token bucket** - O(1) memory, approximate; see module15 P07 and module17.
- Per-key `synchronized(deque)` keeps contention local; the CHM only guards the key -> deque mapping.
- `purgeIdleKeys()` matters: without it the map grows with every customer id ever seen.

## Key Takeaways
- Use `compute*/merge/putIfAbsent/replace` for compound actions; never get-then-put.
- Functions passed to `compute*` must be short, side-effect-free and must **not** touch the same map (deadlock / `IllegalStateException: Recursive update`).
- `size()`, `isEmpty()`, iteration on CHM are weakly consistent; fine for monitoring, not for money.
- Bounded blocking queues give back-pressure; unbounded ones trade it for an OOM risk.
- `CopyOnWriteArrayList` for listeners/rules; `Collections.synchronizedList` or a lock for write-heavy lists.
- `LongAdder` beats `AtomicLong` under heavy contention for counters you only read occasionally.

## Cheat Sheet
| Need | Pick |
|---|---|
| Concurrent map | `ConcurrentHashMap` (never `Hashtable`/`synchronizedMap` for throughput) |
| Sorted concurrent map | `ConcurrentSkipListMap` |
| Bounded producer/consumer | `ArrayBlockingQueue` / `LinkedBlockingQueue(capacity)` |
| Lock-free unbounded queue | `ConcurrentLinkedQueue` |
| Hand-off | `SynchronousQueue` |
| Delayed delivery | `DelayQueue` |
| Read-mostly list/set | `CopyOnWriteArrayList` / `CopyOnWriteArraySet` |
| Hot counter | `LongAdder` |

## 10 Interview Q&A
1. **How does CHM achieve thread safety in Java 8+?** CAS on bin heads for inserts into empty bins, `synchronized` on the first node of a non-empty bin, no global lock; reads are lock-free volatile reads.
2. **Why can't you `put(k, get(k)+1)` on CHM?** Two separate atomic operations; a writer may interleave. Use `merge`.
3. **Can `computeIfAbsent` deadlock?** Yes if the mapping function modifies the same map (recursive update) or blocks for long - it holds the bin lock.
4. **Is `size()` accurate on CHM?** It is an estimate under concurrent modification (uses `LongAdder`-style counter cells).
5. **BlockingQueue `put` vs `offer` vs `add`?** `put` blocks, `offer` returns false (or times out), `add` throws when full.
6. **How do you stop consumers cleanly?** Poison pill per consumer, or interrupt + `take()` throws `InterruptedException`.
7. **When is `CopyOnWriteArrayList` wrong?** Frequent writes or large lists: each write is O(n) allocation.
8. **Sliding window vs token bucket?** Sliding log is exact but O(limit) memory; token bucket is O(1) and allows controlled bursts.
9. **How do you make an idempotent payment endpoint?** Client sends an idempotency key; server claims it atomically (`compute`/`putIfAbsent` or a DB unique constraint) and returns the stored response for retries.
10. **`LongAdder` vs `AtomicLong`?** `AtomicLong` CAS-spins on one cell; `LongAdder` stripes across cells and sums on read. Prefer `LongAdder` for write-heavy counters.

## Exercises for the reader
1. Add a `transfer(from, to, amount)` to `BalanceStore` that is atomic across two keys (hint: lock ordering or a two-phase reserve).
2. Make `IdempotencyCache` store in-flight markers so a second caller waits on a `CompletableFuture` instead of blocking inside `compute`.
3. Replace the deque in `SlidingWindowRateLimiter` with a fixed-size ring buffer and measure the allocation difference.
