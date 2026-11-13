# Overall Cheat Sheet - the one page before the interview

## Money and values
- `BigDecimal` with explicit scale (`setScale(2, HALF_EVEN)`); `compareTo` for comparison, normalise scale for `equals`.
- Records for value objects; validate and copy collections in the compact constructor; typed IDs (`record AccountId(String value)`).
- Immutable: `final` class, `final` fields, no setters, `List.copyOf`, `java.time` instead of `Date`.

## equals/hashCode
Override both with the same fields; `final` classes or records to keep symmetry; never mutate a key after insertion; `BigDecimal(1.0) != BigDecimal(1.00)`.

## Collections
`ArrayList` default, `ArrayDeque` for stack/queue, `HashMap` O(1) (power-of-two table, spread hash, treeify at 8 nodes / 64 buckets, resize at 0.75),
`TreeMap` O(log n) with `floorEntry/subMap`, `LinkedHashMap(accessOrder)` = LRU, `PriorityQueue` heap. Pre-size maps with `expected/0.75`.

## Concurrent collections
One CHM call = one atomic step: `merge`, `compute*`, `putIfAbsent`, `replace(k, old, new)`. Never get-then-put. `size()` is an estimate.
`ArrayBlockingQueue` for back-pressure, `ConcurrentLinkedQueue` lock-free unbounded, `CopyOnWriteArrayList` read-mostly, `LongAdder` for hot counters.

## Generics
Invariant (`List<Card>` is not `List<Tx>`); PECS (`? extends` read, `? super` write); `T extends Comparable<? super T>`; erasure => type tokens (`Class<T>`), no `new T[]`; `Result<T,E>` sealed for expected failures.

## Streams
Lazy, single-use, vertical processing; `sorted/distinct` are barriers; `reduce` needs identity + associativity; `toMap` needs a merge fn and rejects null values;
`groupingBy(key, downstream)`, `partitioningBy`, `teeing`; parallel only for big CPU-bound in-memory data with an associative reduction; `count()` may skip `map()`.

## Modern Java
Records, sealed interfaces, `instanceof` patterns, record patterns + `switch` patterns (21), guards (`when`), `case null`, switch expressions with `yield`, text blocks (`formatted`, `\`, `\s`), `List.of` (immutable, null-hostile), `var`, `Stream.toList()`.

## Exceptions
Checked = caller can recover; unchecked = bugs/business violations; `Error` = fail fast. Always chain the cause; never swallow; restore interrupts; try-with-resources closes in reverse order and suppresses close() failures. Domain exceptions carry `code` + `retryable`. Circuit breaker CLOSED -> OPEN -> HALF_OPEN; bounded jittered retries only for retryable + idempotent.

## Concurrency fundamentals
`synchronized` = atomicity + visibility; `volatile` = visibility only (`count++` still races); atomics = CAS. `wait()` in a `while`, `notifyAll()`. Private lock objects. Lock ordering for multiple locks. `CountDownLatch` one-shot, `CyclicBarrier` reusable, `Semaphore` permits. Executors: submit `Callable`, read `Future.get` for exceptions; shutdown -> awaitTermination -> shutdownNow.

## JMM
Happens-before via: lock/unlock, volatile write/read, `start()`, `join()`, j.u.c. synchronisers, `final` fields after construction. DCL needs `volatile` (or holder idiom). CAS loops must be pure and short. ABA -> `AtomicStampedReference`. `Integer == Integer` only within -128..127.

## Locks and pools
`ReentrantLock` for tryLock/timeouts/conditions; `ReadWriteLock` read-mostly (no upgrade, downgrade OK); `StampedLock` optimistic reads (not reentrant). Deadlock = mutual exclusion + hold-and-wait + no pre-emption + circular wait; detect with `jstack`/`ThreadMXBean`. `ThreadPoolExecutor`: core -> queue -> max (only when queue full) -> reject; bounded queue + `AbortPolicy` for APIs; size CPU = cores+1, I/O = cores * util * (1 + wait/compute).

## CompletableFuture
`thenApply` map, `thenCompose` flatMap, `thenCombine` zip, `allOf` + join, `anyOf`; `exceptionally/handle/whenComplete/exceptionallyCompose`; `orTimeout/completeOnTimeout` (orTimeout returns `this`); always pass an executor for blocking work; recover per branch before fan-in; never `join()` inside a stage.

## Java 21 concurrency
Virtual threads: one per task, never pool, cheap blocking, `Semaphore` for downstream limits; pinning on `synchronized` + blocking (use `ReentrantLock`). `StructuredTaskScope.ShutdownOnFailure/ShutdownOnSuccess`, `joinUntil`, `throwIfFailed`. `ScopedValue` replaces request-scoped `ThreadLocal`.

## JVM
Class init on first active use (constants excluded), parent-first loaders. Tiered JIT (C1/C2), monomorphic inlining, warm-up, dead-code elimination; use JMH. String pool and `intern`; `StringBuilder` in loops. GC: allocation rate drives young GC; avoid boxing/varargs/streams in hot loops; keep objects short-lived or long-lived. False sharing -> padding/striping. Contention -> striping/CHM/shorter critical sections.

## Fintech patterns (modules 15-17)
Idempotency key claimed atomically (`putIfAbsent`/unique constraint); typed outcomes; ordered locks for two-account transfers; double-entry ledger (per-currency zero sum, append-only journal, reconcilable projection); token bucket / sliding window / fixed window trade-offs; versioned immutable rate snapshots; injected clocks everywhere; latches + exact totals in tests.

## Phrases that score
"This is a check-then-act, so `computeIfAbsent`." - "Lock ordering by account id prevents the A->B/B->A deadlock." - "`volatile` gives visibility, not atomicity." - "Money is `BigDecimal` with explicit scale." - "I inject the clock so the test is deterministic." - "Expected outcomes are typed results; exceptions are for bugs."
