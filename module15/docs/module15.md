# Module 15 - Senior Backend Coding Problems (P01-P15)

Package root: `module15.pNN<topic>`. Each problem is a self-contained class with a test that proves the contract,
error paths and (where relevant) exact results under N threads. These are the 45-minute live-coding shapes Revolut
and similar fintechs use; the point is to produce correct, tested, idiomatic code while talking through trade-offs.

| # | Problem | Core idea | Concurrency proof |
|---|---|---|---|
| P01 | `P01ThreadSafeCounter` | `AtomicLong`, CAS loop for "never below zero" | 8 threads x 20k exact |
| P02 | `P02AccountBalance` | BigDecimal, overdraft limit, immutable statement | - |
| P03 | `P03MoneyTransfer` | per-account `ReentrantLock`, lock ordering, `Result` enum | 6 opposing threads, money conserved, no deadlock |
| P04 | `P04PaymentStateMachine` | explicit `EnumMap` transition table, final states | - |
| P05 | `P05IdempotentPaymentService` | `computeIfAbsent` claim + `CompletableFuture` for concurrent duplicates, payload conflict | 32 duplicates -> 1 execution |
| P06 | `P06EventDeduplicator` | `compute` with TTL, purge | 32 callers -> 1 winner |
| P07 | `P07TokenBucketRateLimiter` | lazy refill, burst capacity, injected nano clock | 16 threads never exceed capacity |
| P08 | `P08LruCache` | HashMap + doubly linked list, O(1), hit/miss metrics | - |
| P09 | `P09BoundedBuffer` | `ReentrantLock` + two `Condition`s, timed offer/poll | 4 producers / 3 consumers exact sum |
| P10 | `P10TransactionAggregator` | immutable `Stats` merged via `CHM.merge` | 8 threads exact count/sum/min/max |
| P11 | `P11BatchProcessor` | flush by size or on demand, deliver outside the lock, retain failed batch in order | 8 threads, every item delivered once |
| P12 | `P12RetryWithBackoff` | exponential backoff, cap, jitter, retryable predicate, injected sleeper | - |
| P13 | `P13DelayedTaskScheduler` | `PriorityQueue` by due time, cancel, fixed rate, controllable clock | - |
| P14 | `P14SlidingWindowStats` | ring of time buckets, O(1) memory | - |
| P15 | `P15TransactionHistory` | `TreeMap` (time, seq) key, range, as-of, keyset pagination | - |

## How to talk through a problem (the rubric)
1. **Clarify** the contract: inputs, invariants, failure modes, concurrency expectations, money type.
2. **Pick the data structure** and say why (and what you rejected).
3. **Write the happy path**, then the invariants (never negative, exactly once, no deadlock).
4. **Test**: deterministic clocks, latches, exact totals. No `Thread.sleep` as synchronisation.
5. **Discuss production**: persistence, metrics, back-pressure, what a library (Caffeine, Resilience4j) would give you.

## BAD vs GOOD patterns that recur
```java
// BAD: check-then-act
if (!seen.containsKey(id)) { seen.put(id, now); return true; }
// GOOD: single atomic step
seen.compute(id, (k, prev) -> ...) / putIfAbsent / computeIfAbsent
```
```java
// BAD: lock order follows arguments -> deadlock
// GOOD: order by id (P03) or tryLock + back-off (module11)
```
```java
// BAD: deliver to the sink while holding the buffer lock (slow sink blocks producers; sink calling back deadlocks)
// GOOD: drain under lock, deliver outside (P11)
```
```java
// BAD: Thread.sleep in retry/scheduler code and tests
// GOOD: inject Sleeper / clock (P12, P13, P14, P06, P07)
```

## Key Takeaways
- Every problem has an invariant; name it, then design the lock/CAS around it.
- Immutable value objects + atomic swap (`merge`, `accumulateAndGet`) beat mutable objects + locks for aggregates.
- Inject time. It makes rate limiters, TTL caches and schedulers testable in milliseconds.
- Deliver, notify and call out **outside** locks.
- Return typed results (`Result` enum, `Optional`) for expected outcomes; throw for programmer errors.
- Know the library equivalents: `ScheduledThreadPoolExecutor`, `LinkedHashMap` LRU / Caffeine, Resilience4j retry/rate limiter, Bucket4j.

## Cheat Sheet
| Need | Structure |
|---|---|
| exactly-once by key | `ConcurrentHashMap.computeIfAbsent` / `putIfAbsent` |
| never-negative counter | CAS loop |
| two-resource atomicity | ordered locks |
| LRU | HashMap + DLL or `LinkedHashMap(accessOrder)` |
| bounded hand-off | `ArrayBlockingQueue` or Lock + 2 Conditions |
| delayed tasks | `PriorityQueue` by due time / `DelayQueue` |
| sliding window | ring of buckets (approximate) or deque of timestamps (exact) |
| time-range queries | `TreeMap.subMap` / `headMap` |
| pagination | keyset cursor, never offset |

## 10 Interview Q&A
1. **Why `AtomicLong` rather than `synchronized` for a counter?** Lock-free CAS scales better under moderate contention and cannot deadlock; `LongAdder` for very high write rates.
2. **How do you make a transfer atomic across two accounts without a database?** Lock both in a global order; or reserve-then-commit with compensation; or a single-writer per shard.
3. **Why a transition table instead of `if` chains for the state machine?** It is data: easy to test exhaustively, render, and extend; illegal transitions fail in one place.
4. **Idempotency key reused with a different body?** Return 409/422; it is a client bug, not a replay.
5. **Token bucket vs sliding log?** Bucket is O(1) memory and allows bursts; log is exact but O(limit) memory per key.
6. **Why deliver batches outside the lock?** A slow or re-entrant sink would otherwise block producers or deadlock.
7. **Why jitter in backoff?** To avoid synchronised retry storms (thundering herd) after an outage.
8. **Fixed-rate vs fixed-delay scheduling?** Fixed rate schedules from the previous due time (catches up); fixed delay from completion (never overlaps).
9. **Why keyset pagination?** Offset pagination skips or repeats rows when new rows arrive; a (time, seq) cursor is stable.
10. **How would you persist P15?** Append-only table indexed on (account, at, seq); balance-as-of via periodic snapshots plus delta replay.

## Exercises for the reader
1. Make `P05IdempotentPaymentService` expire stored responses after 24 h using the deduplicator pattern from P06.
2. Add `P08LruCache.getOrCompute(key, loader)` that computes at most once per key under concurrency.
3. Give `P13DelayedTaskScheduler` a worker thread that sleeps `nanosUntilNext()` and wakes on new schedules (hint: `Condition.awaitNanos`).
