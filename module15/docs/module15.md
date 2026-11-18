# Module 15 - Senior Backend Coding Problems (P01-P15)

Package root: `module15.pNN<topic>`. Each problem is a self-contained class with a test that proves the contract,
error paths and (where relevant) exact results under N threads. These are the 45-minute live-coding shapes Revolut
and similar fintechs use; the point is to produce correct, tested, idiomatic code while talking through trade-offs.

## Overview
This module is the bridge between "knows the APIs" and "can build a correct backend component under time pressure".
Each problem is a self-contained class with a test suite proving its contract, edge cases and, where relevant,
behaviour under concurrency. P01-P15 cover counters, balances, transfers, state machines, idempotency, deduplication,
rate limiting, caching, buffering, aggregation, batching, retry, scheduling, windows and history. **P16** (transactional
outbox + idempotent consumer) and **P17** (per-key ordered execution) add the two patterns behind most real-world
"exactly once" and "in order" guarantees.

## Learning objectives
- Turn a vague prompt into a precise contract (invariants, failure modes, concurrency expectations).
- Choose and justify data structures and synchronisation, and test them with exact invariants.
- Explain at-least-once vs effectively-once, and build both halves (outbox relay, idempotent consumer).
- Preserve per-entity ordering while scaling across entities.

## Prerequisites
- Modules 03, 04, 09-12. Module08 for error semantics.

## Java version notes
All problems compile with `--release 17`: records, sealed types, `instanceof` patterns, switch expressions, `Stream.toList()`.
Nothing here requires Java 20/21. With Java 21 you could implement P17's workers as virtual threads without changing the ordering logic.

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
| P16 | `P16TransactionalOutbox` | outbox row in the same transaction, relay, idempotent consumer | failure injection: crash after publish -> duplicate delivery -> deduplicated |
| P17 | `P17PerKeyOrderedExecutor` | per-key `CompletableFuture` chain via `CHM.compute`, failure isolation, key cleanup | 16 accounts x 500 events, strict per-account order, parallel across accounts |

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

---

## Production deep dive

### P16: transactional outbox + idempotent consumer
```mermaid
sequenceDiagram
    participant S as Payment service
    participant DB as Database
    participant R as Outbox relay
    participant B as Broker
    participant C as Consumer
    S->>DB: BEGIN; debit balance; INSERT outbox(event); COMMIT
    R->>DB: SELECT unpublished ORDER BY id
    R->>B: publish(event 1)
    Note over R: crash before markPublished
    R->>DB: SELECT unpublished (event 1 again)
    R->>B: publish(event 1) - duplicate
    B->>C: event 1, event 1
    C->>C: processed_ids already has 1, so ignore
```
**Why it matters.** "Update the DB and publish an event" is the most common distributed-consistency bug in payment systems.
Two-phase commit across DB and broker is rarely available or desirable; the outbox pattern gives atomicity with only a local transaction.
**Tests prove:** a crash before commit leaves neither balance change nor event; a crash after publish but before marking
causes a duplicate delivery, which the consumer ignores; a broker outage loses nothing (the row stays unpublished);
delivery order follows commit order.
**What this simulation does NOT give you.** Durability (it is in-memory), multiple relay instances (production needs a
lease or `SELECT ... FOR UPDATE SKIP LOCKED`), and ordering across partitions. Consumers must keep their dedup table in
the same transaction as their side effect. A dedup set in memory is lost on restart.

### P17: per-key ordered executor
**Why it matters.** Account events must be applied in order ("deposit then withdraw"), yet a bank has millions of accounts
that should be processed in parallel. Kafka gives this via partitioning by account id. Inside a service (e.g. after
consuming a batch), this executor does the same.
**Mechanism.** `ConcurrentHashMap.compute` atomically appends a task to the account's tail future. Failures are isolated
(`exceptionally(t -> null)` before chaining), so one poison event does not stall the account, and the failure still reaches its submitter.
Idle keys are removed with `remove(key, future)`.
**Tests prove:** 16 accounts x 500 events stay in strict per-account order, with observed parallelism across accounts, and no leaked keys.
**Trade-offs.** A slow event delays only its own account (head-of-line blocking per key). Poison events should go to a
dead-letter store rather than being silently skipped when ordering semantics require it, which is a business decision.

## Delivery and ordering guarantees cheat sheet
| Guarantee | Where it comes from | Where it breaks |
|---|---|---|
| Atomic state + event | outbox row in the same DB transaction | publishing directly to the broker from the service |
| At-least-once delivery | relay marks published *after* send | marking before send (at-most-once, loses events) |
| Effectively-once effect | consumer dedup by event id **in the same transaction as its effect** | in-memory dedup, or dedup committed separately |
| Per-key order | single writer per key (partition / P17) | consumer-side thread pools without key affinity; retries that reorder |
| Cross-service consistency | sagas with compensations, reconciliation | assuming in-memory locks span services |

## Fixed in review: P05 payload comparison
`P05IdempotentPaymentService` compared payloads with record `equals`, which compares `BigDecimal` by scale: a client retrying
`10.00` after `10.0` got a false `ConflictException`. Payloads are now compared with `compareTo` on the amount. Also, duplicate
callers waiting on a failed original now receive the original exception, not a `CompletionException` wrapper.

## Common pitfalls
- Calling the broker inside the DB transaction (holds locks during network I/O; the event can still be lost on rollback).
- Relay without ordering, or several relays without coordination, so events go out of order or duplicated beyond what the consumer can handle.
- Consumers that are "idempotent" only for exact duplicates but not for *reordered* events (use sequence numbers or versions).
- Retrying a failed event for one key while later events for the same key proceed, which silently breaks ordering.
- Unbounded per-key queues: one hot merchant account accumulates millions of pending events. Add back-pressure.

## Production checklist
- [ ] Every state change that others must know about writes an outbox row in the same transaction.
- [ ] Relay is single-active (lease) or uses `SKIP LOCKED`, publishes in id order, and is monitored for lag.
- [ ] Consumers deduplicate durably and handle out-of-order delivery (version checks).
- [ ] Ordering scope (per account, per payment) is explicit and enforced by partitioning or a per-key executor.
- [ ] Dead-letter handling and reconciliation exist for events that can never be processed.
