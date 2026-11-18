# Overall Cheat Sheet - 5-hour review before the technical interview

A timed, self-testing review of the whole repository (modules 01-17 + `explanations`) for a senior/staff Java backend
interview in fintech. Every block has: **key facts**, **code to recognise**, **traps**, a **self-quiz** (answers hidden,
so try first), and **where to look** in the repo if something feels shaky.

> Rules for the 5 hours: read actively (say answers out loud), type the live-coding templates from memory, do not open new
> topics in the last 30 minutes, take the breaks. If a block takes longer, cut its "if time" items, never the breaks.

---

## The schedule

| Time | Block | Topic | Repo |
|---|---|---|---|
| 0:00-0:10 | 0 | Interview format, golden rules, how to talk | module16 `docs/01` |
| 0:10-0:50 | 1 | Money, value objects, equals/hashCode, immutability, collections | 01, 02, 03 |
| 0:50-1:35 | 2 | Concurrency fundamentals, JMM, volatile, atomics, CAS | 09, 10 |
| 1:35-2:10 | 3 | Concurrent collections, locks, deadlocks, thread pools | 04, 11 |
| 2:10-2:20 | - | **Break** (walk, water, no screen) | |
| 2:20-2:55 | 4 | CompletableFuture, resilience, virtual threads, structured concurrency | 08, 12, 13 |
| 2:55-3:25 | 5 | Modern Java 17/21, generics, streams, exceptions | 05, 06, 07, 08, explanations |
| 3:25-3:45 | 6 | JVM, GC, JIT, performance | 14 |
| 3:45-3:50 | - | **Break** | |
| 3:50-4:30 | 7 | Live-coding templates: type them from memory | 15, 16, 17 |
| 4:30-4:50 | 8 | System design: payments, ledger, outbox, idempotency, scaling | 15, 16 `docs/08-09` |
| 4:50-5:00 | 9 | Rapid-fire round, phrases that score, final checklist | this file |

Progress tracker: - [ ] 0 - [ ] 1 - [ ] 2 - [ ] 3 - [ ] 4 - [ ] 5 - [ ] 6 - [ ] 7 - [ ] 8 - [ ] 9

---

## Block 0 (10 min) - Format and golden rules

**Typical loop:** screening -> live coding (60-90 min, your IDE, tests expected) -> Java deep dive (they push until you reach your
edge) -> system design (payments, ledger, exactly-once) -> behavioural.

**Live-coding scoring order:** correctness and invariants -> tests -> clarity -> concurrency awareness -> communication.
Working code with one missing edge case beats a perfect design without running tests.

**The 6-step rhythm (say it, then do it):**
1. Restate the problem; ask about money type, currencies, concurrency, idempotency, scale.
2. List **invariants** out loud ("balance never negative", "money conserved", "each request applied once").
3. Pick data structures and the synchronisation mechanism; say what you rejected and why.
4. Smallest end-to-end slice + one test; run it.
5. Add edge cases, then the concurrent test (latch start, timeout finish, exact totals).
6. Close with production notes: DB constraints, outbox, metrics, what you skipped.

**When you do not know:** "I haven't used X in production; my understanding is ... and I would verify by ...". Never bluff
numbers or API names.

---

## Block 1 (40 min) - Money, values, equals/hashCode, collections

### Money (modules 01, 02, 05, 14)
- `BigDecimal` always; `new BigDecimal("0.10")` or `BigDecimal.valueOf(double)`, **never** `new BigDecimal(0.1)` (= 0.1000000000000000055...).
- Compare with `compareTo`; `equals` is scale-sensitive: `1.0` != `1.00`. Normalise scale in the constructor (`setScale(currencyDigits, ...)`).
- `divide(x)` throws on 1/3: always `divide(x, scale, RoundingMode.HALF_EVEN)`. Round **once**, at the end.
- `HALF_EVEN` (banker's) is the default for fairness; `DOWN` when the institution must never pay out unpriced fractions (FX payouts). State the policy.
- Currency scale: EUR/USD/GBP 2, JPY 0, BHD/KWD 3 (`Currency.getDefaultFractionDigits()`).
- Splitting 100.00 / 3 -> 33.34 + 33.33 + 33.33 (largest remainder), never lose a cent (module02 `MoneyAllocator`).
- Hot paths may use `long` minor units + `Math.multiplyExact` (module14 `MinorUnitFees`), property-tested against `BigDecimal`.
- Messages and files: `toPlainString()` (never `1E+3`). DB: `NUMERIC(19,4)` + currency column, never `FLOAT`.

### Value objects and immutability
- Records: final, `equals/hashCode/toString` from all components, compact constructor for validation.
- Records are **shallowly** immutable: `List.copyOf` in the compact constructor; arrays must be cloned in **and** out, and need
  `Arrays.equals/hashCode` overrides (module02 `SignedSettlementBatch`).
- A record's default `toString` prints secrets: override it to mask PANs (module02 `CardNumber`).
- Immutable class recipe: `final` class, `private final` fields, no setters, defensive copies in/out, `java.time` not `Date`, no `this` escape.
- `Collections.unmodifiableList` is a **view**; `List.copyOf` is a **copy**. `List.of` rejects `null`.
- Typed IDs: `record AccountId(String value)` so you cannot pass a wallet id as an account id.

### equals/hashCode contract
Reflexive, symmetric, transitive, consistent, `x.equals(null) == false`; equal objects **must** have equal hash codes.

```java
// BUG: equals without hashCode -> HashSet.contains(copy) == false, duplicates in maps
// BUG: mutating a key after put -> entry orphaned in old bucket, get() == null, memory leak
// BUG: subclass adds a field to equals -> asymmetric; fix: final classes / records / getClass()
```

### Collections - choose in 10 seconds
| Need | Use | Cost |
|---|---|---|
| List, index, iteration | `ArrayList` (grows 1.5x) | O(1) get, amortised O(1) add |
| Stack / queue / deque | `ArrayDeque` (never `Stack`, rarely `LinkedList`) | O(1) ends |
| Priority | `PriorityQueue` (heap; **iteration is not sorted**) | O(log n) offer/poll |
| Key lookup | `HashMap` | O(1) avg |
| Sorted keys, ranges, as-of | `TreeMap` (`floorEntry`, `ceilingEntry`, `subMap`, `tailMap`) | O(log n) |
| Insertion/access order, LRU | `LinkedHashMap(cap, 0.75f, true)` + `removeEldestEntry` | O(1) |
| Enum keys | `EnumMap` / `EnumSet` | array-backed |

### HashMap internals (numbers they ask for)
- Default capacity 16, load factor 0.75, resize when `size > capacity x 0.75`, capacity always a power of two.
- `hash = h ^ (h >>> 16)`; index `= (n - 1) & hash`.
- Bucket becomes a red-black tree at **8** nodes if the table has at least **64** buckets (otherwise it resizes); back to a list at **6**.
- Treeified lookups are O(log n) only if keys are `Comparable`.
- Resize splits each bucket into "lo" and "hi" lists, no rehash needed.
- Pre-size: `new HashMap<>((int) (expected / 0.75f) + 1)`; `new HashMap<>(expected)` still resizes.
- `HashMap`: one `null` key allowed; `ConcurrentHashMap` and `Hashtable`: no nulls. Iteration order unspecified and changes after resize.

### Fintech applications
- Reconciliation = hash join: index statement by reference, walk the ledger once -> O(n + m) (module03 `StatementReconciler`).
- Balance as of a date = `TreeMap.floorEntry`; back-valued posting updates `tailMap(valueDate, true)` (module03 `BalanceHistory`).

### Self-quiz
<details><summary>1. Why does <code>new BigDecimal("2.50").equals(new BigDecimal("2.5"))</code> return false, and what do you do about it?</summary>
Scale is part of equality (250x10^-2 vs 25x10^-1). Use <code>compareTo</code> for comparisons and normalise scale in the value object so equals/hashCode agree with compareTo.
</details>
<details><summary>2. A HashMap lookup returns null for a key you just inserted. Three possible causes?</summary>
hashCode not overridden (or inconsistent with equals); key mutated after insertion; equals broken (e.g. asymmetric or wrong parameter type <code>equals(Account)</code> overload instead of override).
</details>
<details><summary>3. Is a record immutable?</summary>
Shallowly. Components are final, but a mutable component (List, array, Date) can still change unless copied in the compact constructor, and arrays must also be copied out.
</details>
<details><summary>4. How do you build an LRU cache in 10 lines?</summary>
<code>LinkedHashMap</code> with <code>accessOrder = true</code> and override <code>removeEldestEntry(e) -> size() > capacity</code>. Not thread-safe: wrap with a lock or use Caffeine in production.
</details>
<details><summary>5. When is TreeMap better than HashMap?</summary>
When you need ordering or range queries: tiered fees (<code>floorEntry(amount)</code>), balance as of a date, time-range history, top-N by key.
</details>

**If shaky, open:** `module02/docs/module02.md`, `module03/.../hashmapinternals/HashMapInternals.java`.

---

## Block 2 (45 min) - Concurrency fundamentals, JMM, atomics

### The three guarantees
| Tool | Atomicity | Visibility | Ordering |
|---|---|---|---|
| `synchronized` / `Lock` | yes (critical section) | yes (unlock happens-before next lock) | yes |
| `volatile` | **only single reads/writes** (also makes `long`/`double` writes atomic) | yes | yes (no reordering across it) |
| `Atomic*` / `VarHandle` CAS | single variable read-modify-write | yes | yes |
| `final` fields | n/a | yes, after constructor completes (if `this` did not escape) | n/a |

`volatile long count; count++` still loses updates: `++` is read + add + write.

### Happens-before edges (memorise)
Program order in one thread - monitor unlock -> later lock of same monitor - volatile write -> later read of same variable -
`Thread.start()` -> actions in the started thread - all actions in a thread -> `join()` returning - `final` field writes ->
any thread that sees the constructed object - every `java.util.concurrent` hand-off (put/take on queues, `Future.get`,
latch countDown -> await, executor submit -> task). Transitive.

### Code to recognise
```java
// BAD: check-then-act / read-modify-write on shared state
if (balance >= amt) balance -= amt;              // two threads both pass the check
// GOOD
synchronized (lock) { if (balance.compareTo(amt) < 0) throw ...; balance = balance.subtract(amt); }

// Conditional CAS loop (credit limit; module10 CreditLimitReserver)
while (true) {
    long cur = reserved.get(), next = cur + amount;
    if (next > limit) return false;                 // reject without side effects
    if (reserved.compareAndSet(cur, next)) return true;  // lost the race -> retry
}

// Consistent multi-field publication (module10 QuoteBook)
record Snapshot(long version, Map<String, Quote> quotes) { Snapshot { quotes = Map.copyOf(quotes); } }
AtomicReference<Snapshot> current;   // writers build a new snapshot, one volatile write; readers one volatile read

// Double-checked locking needs volatile (or use the holder idiom)
private static volatile Config instance;
```

### Threads and coordination
- Lifecycle: NEW -> RUNNABLE -> BLOCKED (monitor) / WAITING / TIMED_WAITING -> TERMINATED.
- Interruption is cooperative: blocking calls throw `InterruptedException` and **clear the flag**; restore with
  `Thread.currentThread().interrupt()` or propagate. CPU loops poll `isInterrupted()`.
- `wait()` always in a `while` loop (spurious wake-ups, stolen conditions); prefer `notifyAll()`; never lock on `this` or a String literal.
- `CountDownLatch` one-shot event count; `CyclicBarrier` reusable meeting point; `Semaphore` permits / admission control; `Phaser` dynamic phases.
- Executors: submit `Callable`, read `Future.get()` (exceptions arrive wrapped in `ExecutionException`); `execute(Runnable)` sends exceptions to the uncaught handler.
- Shutdown: `shutdown()` -> `awaitTermination()` -> `shutdownNow()` -> `awaitTermination()`.
- `ScheduledExecutorService`: **one uncaught exception silently cancels all future runs** -> wrap with try/catch + metric (module09 `ResilientScheduler`).
- `ThreadLocal` on pools: lost context (pool thread empty) and leaked context (not removed) -> capture at submit, restore and clear in `finally` (module09).

### Lock-free notes
- CAS failure under contention = retries (CPU). Very hot counters: `LongAdder` (striped cells, sum on read).
- ABA: value goes A -> B -> A and a CAS on a reference succeeds wrongly; fix with `AtomicStampedReference` or never reuse nodes. Not an issue for plain counters.
- CAS functions in `updateAndGet`/`accumulateAndGet` may run several times: must be side-effect free.

### Self-quiz
<details><summary>1. Why does a reader thread spin forever on a plain <code>boolean running</code>?</summary>
No happens-before edge: the JIT may hoist the read out of the loop or the CPU may keep a stale value. Make it <code>volatile</code> (or use an AtomicBoolean / lock).
</details>
<details><summary>2. Two volatile fields bid and ask, updated one after the other. What can a reader see?</summary>
A torn pair: new bid with old ask, even a crossed market (bid > ask). Volatile is per field. Publish one immutable snapshot through a single volatile/AtomicReference.
</details>
<details><summary>3. What does <code>synchronized</code> give you that <code>volatile</code> does not?</summary>
Mutual exclusion, so compound actions (check-then-act, read-modify-write, multi-field updates) are atomic.
</details>
<details><summary>4. Why is <code>Thread.sleep</code> not a synchronisation tool in tests?</summary>
No happens-before guarantee and timing-dependent: slow and flaky. Use latches, futures with timeouts, and assert invariants.
</details>
<details><summary>5. What is safe publication? Give three ways.</summary>
Making a fully constructed object visible to other threads with a happens-before edge: store into a volatile/atomic field, a final field of an object that is itself safely published, a concurrent collection, or under a lock; static initialiser also works.
</details>
<details><summary>6. When would you pick LongAdder over AtomicLong?</summary>
Many writers, rare reads (metrics counters). Not when you need a conditional update or an exact value on every read (limits, balances).
</details>

**If shaky, open:** `module10/docs/module10.md`, `module09/docs/module09.md`.

---

## Block 3 (35 min) - Concurrent collections, locks, deadlocks, pools

### The one rule
**A thread-safe collection makes each call atomic, not your sequence of calls.**
```java
// BAD on ConcurrentHashMap too
Integer c = map.get(k); map.put(k, c == null ? 1 : c + 1);
if (!map.containsKey(id)) map.put(id, v);
// GOOD
map.merge(k, 1, Integer::sum);
map.putIfAbsent(id, v);                          // idempotency claim: null return == you won
map.computeIfPresent(id, (key, bal) -> { if (bal.compareTo(a) < 0) throw ...; return bal.subtract(a); });
map.replace(id, expected, updated);              // CAS on the value
```
- CHM (Java 8+): CAS into empty bins, `synchronized` on the bin head otherwise, lock-free reads, no global lock, no null keys/values.
- `size()`, `isEmpty()`, iteration: **weakly consistent** (no `ConcurrentModificationException`, may miss concurrent updates). Fine for metrics, never for money totals.
- `compute*` functions: short, no I/O, **never touch the same map** (`IllegalStateException: Recursive update` or deadlock).
- Memoizer without stampede: store `CompletableFuture`s, install atomically, load **outside** the bin lock, remove failed futures (module04 `FxRateCache`).

| Need | Pick |
|---|---|
| Concurrent map / sorted map | `ConcurrentHashMap` / `ConcurrentSkipListMap` |
| Bounded producer-consumer with back-pressure | `ArrayBlockingQueue` / `LinkedBlockingQueue(capacity)` |
| Lock-free unbounded queue (no back-pressure!) | `ConcurrentLinkedQueue` |
| Direct hand-off | `SynchronousQueue` |
| Delayed retries | `DelayQueue` (`drainTo` only moves **expired** elements) |
| Read-mostly listeners / rules | `CopyOnWriteArrayList` (O(n) per write, snapshot iteration) |
| Hot counter | `LongAdder` |

`BlockingQueue`: `put` blocks, `offer` returns false (or waits with timeout), `add` throws. Stop consumers with a poison pill per consumer or interruption.

### Locks
| Lock | When | Watch out |
|---|---|---|
| `synchronized` | simple critical section | no timeout, not interruptible |
| `ReentrantLock` | `tryLock(timeout)`, `lockInterruptibly`, fairness, several `Condition`s | `unlock()` in `finally` |
| `ReentrantReadWriteLock` | read-mostly data; consistent snapshots (transfers take read, snapshot takes write) | no read->write upgrade (self-deadlock); downgrade OK |
| `StampedLock` | very read-heavy, optimistic reads | not reentrant, no conditions |

`Condition` rules: `await` in a `while`, `signalAll` when waiters wait for different predicates, use `awaitNanos(remaining)` for deadlines (module11 `FundingAwaitAccount`).

### Deadlock, livelock, starvation
- Coffman conditions: mutual exclusion + hold-and-wait + no pre-emption + circular wait. Break circular wait: **global lock order by account id**.
- Alternative: `tryLock(timeout)` + release all + **jittered** back-off (no jitter -> livelock).
- Detect: `jstack <pid>` / `jcmd <pid> Thread.print` ("Found one Java-level deadlock"), `ThreadMXBean.findDeadlockedThreads()` in a watchdog (module11 `DeadlockDetector`). Threads stuck in `synchronized` cannot be interrupted; `lockInterruptibly` can.
- Starvation: unfair locks, saturated pools; fair locks cost throughput.
- Thread-pool deadlock: tasks blocking on sub-tasks queued in the same bounded pool.

```java
// Two-account transfer (module16 TransferService, module04 InMemoryDoubleEntryLedger)
ReentrantLock first = lockFor(min(from, to)), second = lockFor(max(from, to));
first.lock(); try { second.lock(); try { /* check, debit, credit */ } finally { second.unlock(); } } finally { first.unlock(); }
```

### ThreadPoolExecutor
- Order: core threads -> **queue** -> extra threads up to max (only when the queue is full) -> rejection.
- Rejection: `AbortPolicy` (throw; map to 429/503 for APIs), `CallerRunsPolicy` (natural back-pressure), `DiscardPolicy`, `DiscardOldestPolicy` (dangerous for payments).
- `newFixedThreadPool`: unbounded `LinkedBlockingQueue` (hidden overload, OOM). `newCachedThreadPool`: unbounded threads. Production: explicit `ThreadPoolExecutor`, bounded queue, named threads, metrics.
- Sizing: CPU-bound about `cores` (+1); I/O-bound `cores x (1 + wait/compute)`, capped by the downstream pool size.

### Self-quiz
<details><summary>1. Why is <code>map.get</code> + <code>map.put</code> broken on ConcurrentHashMap if CHM is thread-safe?</summary>
Each call is atomic, the pair is not: another thread can write between them, so updates are lost. Use merge/compute/putIfAbsent.
</details>
<details><summary>2. How do you get a consistent total of all balances while transfers run?</summary>
Iterating a CHM is weakly consistent and can see a debit without its credit. Use a read-write gate (transfers hold read, snapshot holds write), an immutable snapshot, or the DB's snapshot isolation.
</details>
<details><summary>3. Your service hangs, CPU is idle. What do you do?</summary>
Take 2-3 thread dumps a few seconds apart (jstack/jcmd), look for BLOCKED threads and a deadlock report, identify lock owners, check DB locks too. Fix with lock ordering or tryLock with timeouts.
</details>
<details><summary>4. Core 4, max 8, queue 100. When does thread #5 start?</summary>
Only after the queue holds 100 tasks and a new task arrives. Many people expect threads to grow first.
</details>
<details><summary>5. When is CopyOnWriteArrayList wrong?</summary>
Write-heavy or large lists: each write copies the array. Right for listeners, fraud rules, configuration read on every request.
</details>

**If shaky, open:** `module04/docs/module04.md` ("When a concurrent collection is NOT enough"), `module11/docs/module11.md`.

---

## Break (10 min)
Stand up, walk, drink water. No screen. Recall the 6-step rhythm from Block 0 while walking.

---

## Block 4 (35 min) - Async, resilience, Java 21 concurrency

### CompletableFuture vocabulary
| Want | Method |
|---|---|
| map | `thenApply` |
| flatMap (async call returning a future) | `thenCompose` |
| combine two | `thenCombine` |
| wait for all / first | `allOf(...)` + `join` each / `anyOf` |
| recover | `exceptionally`, `exceptionallyCompose` (12) |
| both paths | `handle`; observe only: `whenComplete` |
| deadline | `orTimeout` / `completeOnTimeout` (9) |
| start | `supplyAsync(supplier, executor)` - **always pass an executor for blocking I/O** |

Traps: callbacks receive `CompletionException` wrappers (unwrap `getCause()`); `orTimeout`/`cancel(true)` complete the future
but **do not stop** the underlying HTTP call; `join()` inside a stage on a bounded pool can deadlock it; default executor is the
common `ForkJoinPool` shared by parallel streams; recover per branch before `allOf` or one failure fails all.

### Resilience layers (outermost first)
| Layer | Why | Repo |
|---|---|---|
| Rate limiter | protect us from clients (429 + `Retry-After`) | 04, 15 P07, 16, 17 |
| Timeout / deadline | bound latency to the caller's SLA | 12 |
| Bulkhead | bound in-flight calls per dependency, fail fast | 12 `AsyncBulkhead` |
| Circuit breaker | CLOSED -> OPEN -> HALF_OPEN; stop calling a dead dependency | 08 |
| Retry | only retryable errors, capped exponential backoff + **full jitter**, max attempts, overall deadline, **same idempotency key** | 12 `AsyncRetry`, 15 P12 |
| Fallback | cached FX rate with staleness limit, "processing" status, manual review | 04, 12 |

**Timeout is not failure (most important payments rule).** The provider may have executed the payment.
Query status by idempotency key -> ACCEPTED/REJECTED: done; NOT_FOUND: resubmit **once** with the same key; still unknown:
park and reconcile. Never tell the customer "failed", never resubmit as a new payment (module08 `AmbiguousOutcomeHandler`).

### Rate limiters
| Algorithm | Memory/key | Accuracy | Bursts |
|---|---|---|---|
| Fixed window | O(1) | up to 2x at boundary | yes, at the boundary |
| Sliding log | O(limit) | exact | no |
| Sliding window counter | O(1) | approximate (weighted previous window) | smoothed |
| Token bucket | O(1) | exact on average | up to capacity |
Distributed: per-node limiters allow N x limit; Redis `INCR`+`EXPIRE` or Lua; decide fail-open vs fail-closed (closed for login/OTP/payments).

### Virtual threads (Java 21 final; preview in 19/20)
- Blocking unmounts the virtual thread from its carrier (small `ForkJoinPool`); millions are cheap.
- One per task: `Executors.newVirtualThreadPerTaskExecutor()` in try-with-resources (`close()` waits).
- **Never pool them**; limit downstream concurrency with a `Semaphore` sized to the DB/provider pool (module13 `BoundedVirtualFanOut`).
- Good: I/O-bound handlers, batch fan-out. Bad: CPU-bound risk/pricing (use about `cores` platform threads), low-latency hot paths.
- Pinning (JDK 21-23): blocking inside `synchronized` or native frames pins the carrier -> use `ReentrantLock`; diagnose with
  `-Djdk.tracePinnedThreads=full` or JFR `jdk.VirtualThreadPinned`. Fixed for `synchronized` in JDK 24 (JEP 491).
- `ThreadLocal` caches multiplied by millions of threads = memory problem.
- Structured concurrency (21 **preview**): `ShutdownOnFailure` (all required, first failure cancels siblings, `throwIfFailed`),
  `ShutdownOnSuccess` (first wins), `joinUntil(deadline)`.
- `ScopedValue` (21 **preview**, final in 25): immutable, scope-bound, inherited by forked subtasks, cannot leak like `ThreadLocal`.

### Self-quiz
<details><summary>1. thenApply vs thenCompose?</summary>
thenApply maps a value (returns U); thenCompose chains a function returning a CompletableFuture&lt;U&gt; and flattens it. Using thenApply there gives CompletableFuture&lt;CompletableFuture&lt;U&gt;&gt;.
</details>
<details><summary>2. Why jitter in retries?</summary>
Without it, all clients that failed together retry together (thundering herd) and knock the recovering service over again.
</details>
<details><summary>3. With virtual threads, why still use a Semaphore?</summary>
Threads are no longer the bottleneck, the DB/provider is. 10,000 virtual threads against a 20-connection pool need explicit admission control and timeouts.
</details>
<details><summary>4. A card authorisation call times out. Exact steps?</summary>
Do not approve, do not decline. Query status by idempotency key; resubmit only on NOT_FOUND with the same key; otherwise mark UNKNOWN, show "processing", reconcile with the provider's settlement file.
</details>
<details><summary>5. Circuit breaker vs retry vs bulkhead?</summary>
Retry absorbs transient faults; breaker stops calls to a failing dependency (fail fast, give it time); bulkhead caps concurrency so one slow dependency cannot exhaust shared resources.
</details>

**If shaky, open:** `module12/docs/module12.md`, `module13/docs/module13.md`, `module08/docs/module08.md` (ambiguity section).

---

## Block 5 (30 min) - Modern Java, generics, streams, exceptions

### Java version map (say this precisely)
| Feature | 17 | 19 / 20 | 21 |
|---|---|---|---|
| Records (16), sealed types (17), `instanceof` patterns (16), switch expressions (14), text blocks (15), `Stream.toList()` (16) | final | final | final |
| `switch` type patterns, guards `when`, `case null` | preview | preview | **final** |
| Record patterns (deconstruction, nested) | - | preview | **final** |
| Virtual threads | - | preview | **final** |
| Structured concurrency | - | incubator | preview |
| Scoped values | - | incubator (20) | preview |
| Sequenced collections (`getFirst`, `reversed`) | - | - | final |

### Sealed + records + exhaustive switch
```java
sealed interface PaymentOutcome permits Success, Failure, Pending {}
return switch (outcome) {                                 // no default: adding a type breaks compilation (good)
    case null -> error();
    case Success(var id, var amount, var auth, var at) -> ok(amount);
    case Failure(var id, var reason, var retryable) when reason == FRAUD -> genericDecline(); // never reveal fraud
    case Failure(var id, var reason, var retryable) when retryable -> processingAndRetry(id);
    case Failure f -> genericDecline();                   // general case after guarded ones (dominance check)
    case Pending p -> processing();
};
```
State machines: one record per state carrying only valid data; pure transition function `(state, command) -> state`
(module07 `TransactionLifecycle`; Java 17 version uses `instanceof` chains without exhaustiveness checking).

### Generics
- Invariance: `List<Card>` is not a `List<PaymentMethod>`.
- **PECS**: producer `? extends T` (read), consumer `? super T` (write); `Comparator<? super T>`; `<T extends Comparable<? super T>>`.
- Erasure: no `new T[]`, no `instanceof List<String>`, type tokens `Class<T>`, bridge methods, heap pollution with generic varargs (`@SafeVarargs`).
- Never return wildcards; isolate and comment each `@SuppressWarnings("unchecked")`.
- Phantom types: `Amount<EUR>` + `Amount<USD>` does not compile (module05); runtime check at the deserialisation boundary.
- `Result<T, E>` sealed `Ok`/`Err` for expected failures.

### Streams
- Lazy, single-use, elements flow one at a time; `sorted`/`distinct` are barriers; nothing runs without a terminal op.
- Money: `reduce(BigDecimal.ZERO, BigDecimal::add)` or `reducing(...)`, never `mapToDouble().sum()`.
- `toMap` throws on duplicate keys (pass a merge function) and NPEs on null values; `groupingBy(k, TreeMap::new, downstream)` for deterministic reports.
- `count()` may skip `map`/`peek` (side effects not run); no side effects in intermediate ops.
- Parallel streams: only large, CPU-bound, associative, measured; they share the common pool; never blocking I/O.
- `Optional`: return type only; `orElseThrow(() -> new DomainException(...))`, not `get()`; not for fields/parameters.

### Exceptions
- Checked = caller can recover; unchecked = programming errors and business violations; `Error` = do not catch, fail fast.
- Translate at boundaries, **keep the cause**, never swallow, restore interrupts, don't double-wrap.
- Domain exceptions carry `code` + `retryable`. Expected outcomes (declined, insufficient funds) are often typed results.
- try-with-resources closes in reverse order; close failures are suppressed (`getSuppressed()`).
- API validation: accumulate all field errors, never echo input values (module08 validator).
- Logs: no PANs/IBANs/emails/balances; escape CR/LF against log forging (module08 `LogSanitizer`); no stack traces to clients.

### Self-quiz
<details><summary>1. What does sealed buy you over an enum or an open interface?</summary>
Each subtype carries its own data (unlike enum constants) while the set stays closed, so switch is exhaustive without default and new subtypes break compilation at every unhandled switch.
</details>
<details><summary>2. Explain PECS with a payments example.</summary>
<code>sum(Collection&lt;? extends Payment&gt; payments)</code> reads (producer); <code>collectFailures(Collection&lt;? super CardPayment&gt; sink)</code> writes (consumer).
</details>
<details><summary>3. Why might a stream with peek(log::info).count() log nothing?</summary>
Since Java 9, count() can compute the size directly from a SIZED source and skip the pipeline, so peek never runs.
</details>
<details><summary>4. Checked or unchecked for InsufficientFunds?</summary>
Either is defensible; modern practice: unchecked domain exception or, better at service boundaries, a typed result (expected business outcome). Say why and be consistent.
</details>

**If shaky, open:** `module07/docs/module07.md`, `explanations/docs/explanations.md`, `module06/docs/module06.md`.

---

## Block 6 (20 min) - JVM, GC, JIT, performance

- **Memory areas:** heap (young: eden + survivors, old), metaspace (class metadata, replaced PermGen in 8), thread stacks, code cache. String pool lives in the heap (since 7).
- **Class loading:** bootstrap -> platform -> application, parent-first delegation; initialisation on first active use; compile-time constants are inlined and do not trigger init; class init is thread-safe (holder idiom).
- **JIT:** interpreter -> C1 -> C2 (hot after about 10k invocations); inlining of monomorphic call sites; megamorphic (3+ types) sites are slower; escape analysis can remove allocations; first requests after deploy are slow (warm-up, CDS/AOT).
- **GC:** most objects die young; allocation rate drives young GC frequency; medium-lived objects get promoted and cause expensive old-gen work.

| GC | Pauses | Fit |
|---|---|---|
| G1 (default since 9) | target via `-XX:MaxGCPauseMillis`, tens of ms | most services |
| ZGC (prod since 15, generational in 21) | sub-millisecond | latency-critical APIs |
| Parallel | longer pauses, best throughput | batch (EOD, statements) |

- **Boxing:** `Integer` cache -128..127 (`==` works by accident inside it); `Long sum` in loops allocates; use primitives/primitive streams.
- **False sharing:** independent hot fields on one 64-byte cache line -> padding / `@Contended` / `LongAdder`-style striping.
- **Contention:** shorter critical sections, striping, CHM, fewer shared writes.
- **Strings:** `+` in loops is O(n^2); `StringBuilder` with capacity; parameterised logging (`log.debug("{}", x)`).
- **Measure:** JMH for micro-benchmarks (warm-up, forks, blackholes; never time in unit tests), JFR (`jcmd <pid> JFR.start duration=60s filename=rec.jfr`), async-profiler, GC logs `-Xlog:gc*`, heap dump `jcmd <pid> GC.heap_dump`.
- **Containers:** `-XX:MaxRAMPercentage`, check `availableProcessors()` vs CPU quota.
- **Investigation order:** SLO (p99) -> reproduce under load -> profile (CPU, alloc, locks, I/O) -> change one thing -> re-measure the same percentile.

<details><summary>Self-quiz: p99 latency spikes every few minutes. Approach?</summary>
Correlate spikes with GC logs (pause times, promotion, full GCs) and JFR; check allocation rate and old-gen growth; check lock contention and pool saturation; then reduce allocation or tune/choose GC (G1 pause target, ZGC). Verify with the same load test.
</details>
<details><summary>Self-quiz: why is a naive benchmark of <code>sumBoxed()</code> misleading?</summary>
No warm-up (measures interpreter/JIT), result may be dead-code eliminated, escape analysis may remove the allocation, one JVM run with no error bars. Use JMH with -prof gc.
</details>

**If shaky, open:** `module14/docs/module14.md`.

---

## Break (5 min)

---

## Block 7 (40 min) - Live-coding templates (type them from memory, then compare)

Give yourself about 8 minutes per template. Compare with the reference file afterwards.

### T1. Thread-safe account (module16 `bank.Account`)
```java
public final class Account {
    private final Object lock = new Object();
    private final AccountId id;
    private Money balance;                                   // guarded by lock
    private final List<Transaction> history = new ArrayList<>(); // guarded by lock

    public void withdraw(Money amount) {
        requirePositive(amount);
        synchronized (lock) {
            if (balance.compareTo(amount) < 0) throw new InsufficientFundsException(id);  // no balance in message
            balance = balance.minus(amount);
            history.add(new Transaction(Kind.WITHDRAWAL, amount, balance));  // same critical section
        }
    }
    public List<Transaction> transactions() { synchronized (lock) { return List.copyOf(history); } }
}
```

### T2. Idempotent, deadlock-free transfer (module16 `TransferService`)
```java
public TransferResult transfer(TransferRequest req) {
    TransferResult prior = processed.putIfAbsent(req.requestId(), IN_FLIGHT);   // atomic claim
    if (prior != null) return duplicateOf(prior);
    TransferResult result = doTransfer(req);   // validate, lock min(id) then max(id), withdraw, deposit
    processed.put(req.requestId(), result);
    return result;
}
```
Say: same id + different payload should be a conflict; idempotency records need a TTL; in a DB this is a `UNIQUE` key + transaction.

### T3. Idempotent payment with concurrent duplicates (module15 P05)
```java
CompletableFuture<Receipt> mine = new CompletableFuture<>();
CompletableFuture<Receipt> existing = inFlight.putIfAbsent(key, mine);
if (existing != null) return existing.join();                 // duplicate waits for the original result
try { mine.complete(process(request)); } catch (RuntimeException e) { mine.completeExceptionally(e); inFlight.remove(key, mine); }
return mine.join();
```

### T4. Token bucket (module15 P07, module16)
```java
public synchronized boolean tryAcquire() {
    long now = clock.getAsLong();                                    // injected nano clock
    tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerNano);
    lastRefill = now;
    if (tokens < 1) return false;
    tokens -= 1;
    return true;
}
```
Per key: `ConcurrentHashMap<String, Bucket>` + `computeIfAbsent`, lock per bucket, purge idle keys.

### T5. LRU cache (module15 P08 / module03)
```java
final class Lru<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    Lru(int capacity) { super(16, 0.75f, true); this.capacity = capacity; }
    @Override protected boolean removeEldestEntry(Map.Entry<K, V> e) { return size() > capacity; }
}
```
Follow-up: hand-rolled HashMap + doubly linked list for O(1) with explicit control; thread-safety via a lock; Caffeine in production.

### T6. Payment state machine (module15 P04, module07)
```java
private static final Map<Status, Set<Status>> ALLOWED = new EnumMap<>(Map.of(
        CREATED, EnumSet.of(AUTHORISED, DECLINED), AUTHORISED, EnumSet.of(CAPTURED, REVERSED),
        CAPTURED, EnumSet.of(REFUNDED), DECLINED, EnumSet.noneOf(Status.class)));
void transition(Status next) {
    if (!ALLOWED.getOrDefault(status, Set.of()).contains(next)) throw new IllegalStateException(status + " -> " + next);
    status = next;
}
```

### T7. The concurrency test you must always write
```java
ExecutorService pool = Executors.newFixedThreadPool(THREADS);
CountDownLatch start = new CountDownLatch(1);
for (int t = 0; t < THREADS; t++) pool.submit(() -> { start.await(); for (...) service.op(); return null; });
start.countDown();
pool.shutdown();
assertTrue(pool.awaitTermination(30, SECONDS), "no deadlock");
assertEquals(EXPECTED_TOTAL, sumOfBalances(), "money conserved");   // exact invariant, no sleeps
```
Bonus points: `@RepeatedTest`, opposite-direction transfers, every request id submitted twice, history replays to balance.

### Live-coding checklist (walk it before saying "done")
- [ ] Shared mutable state identified; every write path behind one lock / CAS / atomic CHM call.
- [ ] No check-then-act, no get-then-put.
- [ ] Multiple locks: global order or `tryLock` + back-off.
- [ ] Short critical sections; no I/O or callbacks under locks.
- [ ] Money: `BigDecimal`, scale, rounding policy, currency check.
- [ ] Inputs validated; typed outcomes for business results.
- [ ] Idempotency key for money-moving operations.
- [ ] Clock injected; tests deterministic; concurrent test with exact totals.
- [ ] Interrupts handled; executors shut down.
- [ ] Production notes ready (next block).

**Build It prompts to rehearse (module17, 45-60 min each):** load balancer, shopping cart, money transfer with daily limit,
subscription proration, currency exchange with stale quotes, URL shortener, tiered rate limiter, double-entry ledger with reversals.
See `REVOLUT_BUILD_IT_PROBLEMS.md`.

---

## Block 8 (20 min) - System design: payments at scale

### What in-memory code can and cannot promise
| Guarantee | In one JVM | Across instances / services |
|---|---|---|
| Atomic balance update | lock / CAS / CHM `compute` | DB transaction, conditional `UPDATE ... WHERE balance >= ?`, row locks in id order |
| Exactly-once effect | `putIfAbsent` claim | `UNIQUE` idempotency key + outbox + idempotent consumers (module15 P16) |
| Per-account ordering | per-key executor (module15 P17) | partition by account id (single writer) |
| Consistent snapshot | read-write gate (module04), immutable snapshot (module10) | DB snapshot isolation |
| Survive restart | nothing | durable store / broker |

**Never claim an in-memory structure guarantees consistency across processes or machines.**

### Core building blocks
- **Double-entry ledger:** every movement debits one account and credits another; legs sum to zero per currency; journal is
  append-only; balances are projections; corrections are reversing entries; reconcile nightly (module17 `Ledger`, module04).
- **Idempotency:** client-supplied key on every write API; unique constraint is the final guard; store the response for replays; reject same key + different payload; TTL.
- **Transactional outbox:** business row + outbox row in one transaction; relay publishes (at-least-once); consumers dedup by
  event id **in the same transaction as their effect** -> effectively once.
- **Delivery semantics:** at-most-once (lossy, not for money) / at-least-once (default) / "exactly-once" = at-least-once + idempotent consumer.
- **Consistency:** strong per account (single writer, partition by account); cross-account or cross-shard via **saga** (reserve -> credit -> confirm, or compensate) + reconciliation for stuck sagas.
- **Locking in the DB:** optimistic `version` column for low contention; `SELECT ... FOR UPDATE` in id order for hot accounts; hot merchant accounts -> sub-accounts or batched credits.
- **Reconciliation:** compare ledger vs bank/scheme statements daily; breaks = amount mismatch, missing, unexpected, duplicate (module03).

### Design answer skeleton (say it in this order)
1. Clarify: TPS, p99 budget, peak factor, consistency between debit and credit, regions, data residency, retention.
2. API: `POST /payments` with `Idempotency-Key`, `GET /payments/{id}` for status (needed for timeouts).
3. Data: accounts, ledger entries (append-only), payments (state + version), idempotency keys, outbox.
4. Flow: validate -> reserve funds (conditional update) -> call provider (timeout, bulkhead, breaker, retry with same key) -> record outcome -> outbox event.
5. Failure modes: provider timeout (unknown outcome, status query, reconcile), DB failover, broker down (outbox holds events), duplicate requests.
6. Scale: shard by account/customer, partition events by account, read replicas for history, cache with explicit staleness.
7. Observability: metrics per outcome, p99, outbox lag, unknown-outcome backlog, reconciliation breaks; traces with correlation id; sanitised logs.
8. Security and regulation: PCI tokenisation, least privilege, maker-checker for manual adjustments, audit trail, retention vs GDPR.

### Questions to ask back
Consistency required between customer debit and merchant credit? Expected TPS and p99, peak multipliers? Data residency and
audit retention? Synchronous confirmation or "processing" acceptable?

<details><summary>Self-quiz: why not publish to Kafka directly after the DB commit?</summary>
Dual write: a crash between commit and publish loses the event; publishing first may announce a payment that rolls back. The outbox puts the event in the same transaction.
</details>
<details><summary>Self-quiz: how do you prevent double spending across two service instances?</summary>
Not with JVM locks. Conditional update in the DB (<code>UPDATE ... SET balance = balance - ? WHERE id = ? AND balance >= ?</code>, check rows affected), or a single writer per account partition, plus a unique idempotency key.
</details>

---

## Block 9 (10 min) - Rapid fire, phrases, final checklist

### 30 rapid-fire questions (answer in one sentence each, out loud)
1. `HashMap` treeify thresholds? -> 8 nodes per bin and 64 buckets; untreeify at 6.
2. Load factor and default capacity? -> 0.75, 16.
3. CHM allows null? -> No keys, no values.
4. `volatile` makes `count++` safe? -> No, visibility only.
5. Two volatile fields consistent? -> No, publish one immutable snapshot.
6. What guarantees `final` fields? -> Visibility after construction if `this` does not escape.
7. DCL needs? -> `volatile` field (or holder idiom).
8. `wait()` in? -> A `while` loop, holding the monitor.
9. `notify` vs `notifyAll`? -> `notifyAll` unless all waiters wait for the same condition.
10. `InterruptedException` caught? -> Restore the flag or propagate.
11. Pool order? -> Core, queue, max, reject.
12. `newFixedThreadPool` risk? -> Unbounded queue.
13. Deadlock fix for transfers? -> Lock in account-id order.
14. Livelock fix? -> Randomised back-off.
15. `ReentrantReadWriteLock` upgrade? -> Not possible; deadlocks.
16. `LongAdder` vs `AtomicLong`? -> Adder for hot write-mostly counters; Atomic for conditional/exact.
17. ABA fix? -> `AtomicStampedReference`.
18. `thenCompose`? -> flatMap for futures.
19. Does `orTimeout` cancel the HTTP call? -> No.
20. Retry needs? -> Retryable classification, backoff + jitter, cap, deadline, idempotency key.
21. Timeout on payment means? -> Unknown outcome: query status, reconcile.
22. Pool virtual threads? -> Never; use a Semaphore.
23. Pinning cause in 21? -> Blocking inside `synchronized` or native frame.
24. Structured concurrency status in 21? -> Preview.
25. Record patterns final in? -> 21 (preview in 19/20).
26. `BigDecimal` compare? -> `compareTo`.
27. Rounding mode for banking? -> `HALF_EVEN` unless the policy says otherwise.
28. `toMap` duplicate key? -> `IllegalStateException`; pass a merge function.
29. Exactly-once? -> At-least-once delivery + idempotent consumer (dedup in the same transaction).
30. Ledger correction? -> New reversing entry; never update history.

### Phrases that score
- "This is a check-then-act, so I'll use `computeIfAbsent` / `putIfAbsent`."
- "I'm ordering locks by account id to prevent the A->B / B->A deadlock."
- "`volatile` gives visibility, not atomicity."
- "Money is `BigDecimal` with explicit scale and a stated rounding policy."
- "I inject the clock so the test is deterministic."
- "Expected outcomes are typed results; exceptions are for bugs."
- "This guarantee holds inside one JVM; across instances I'd rely on a unique constraint and an outbox."
- "A timeout is an unknown outcome, so I'd query status by idempotency key before doing anything else."
- "Retries are bounded, jittered and idempotent."
- "I'd measure p99 with JFR before optimising."

### Security and regulatory reflexes
Mask PANs/IBANs in `toString` and logs, escape CR/LF - never echo invalid input in errors - never reveal fraud reasons to clients
- maker-checker for high-value or manual actions - append-only ledger, corrections as reversals - escape untrusted text in
generated XML/JSON/SQL - tokenise card data at the edge - least privilege and audit of privileged actions.

### Last 30 minutes before the interview
- [ ] Do not learn anything new. Re-read Block 0 and the phrases above.
- [ ] IDE open, new project with JUnit 5 (and AssertJ) compiling, font size readable when screen-sharing.
- [ ] One test template ready (Block 7 T7).
- [ ] Water, quiet room, camera and microphone checked.
- [ ] Three behavioural stories ready (ownership, a production incident you fixed with metrics, a disagreement resolved).
- [ ] Two questions for the interviewer (team's ledger/consistency approach, how incidents are handled).

---

## Appendix A - Complexity quick table
| Structure | get / contains | add / put | remove | notes |
|---|---|---|---|---|
| `ArrayList` | O(1) index, O(n) contains | amortised O(1) end | O(n) | cache friendly |
| `ArrayDeque` | - | O(1) ends | O(1) ends | no nulls |
| `LinkedList` | O(n) | O(1) ends | O(1) with iterator | poor locality |
| `HashMap` / `HashSet` | O(1) avg | O(1) avg | O(1) avg | O(log n) worst after treeify |
| `TreeMap` / `TreeSet` | O(log n) | O(log n) | O(log n) | ordered, ranges |
| `PriorityQueue` | O(1) peek, O(n) contains | O(log n) | O(log n) poll | unsorted iteration |
| `ConcurrentHashMap` | O(1) lock-free read | O(1) | O(1) | weakly consistent iteration |
| `CopyOnWriteArrayList` | O(1) | O(n) | O(n) | snapshot iterators |

## Appendix B - Diagnostics commands
```bash
jcmd                                   # list JVMs
jcmd <pid> Thread.print                # thread dump (look for BLOCKED, "Found one Java-level deadlock")
jcmd <pid> GC.heap_info                # heap summary
jcmd <pid> GC.heap_dump /tmp/heap.hprof
jcmd <pid> JFR.start duration=60s filename=rec.jfr
jstat -gcutil <pid> 1000               # GC utilisation every second
java -Xlog:gc*:file=gc.log ...         # GC logging
java -Djdk.tracePinnedThreads=full ... # virtual thread pinning (JDK 21-23)
```

## Appendix C - Where everything lives
| Topic | Module doc |
|---|---|
| OOP, SOLID, DDD, hexagonal, maker-checker | `module01/docs/module01.md` |
| equals/hashCode, value objects, PAN masking, money allocation | `module02/docs/module02.md` |
| Collections, HashMap internals, reconciliation, as-of balances | `module03/docs/module03.md` |
| Concurrent collections, DelayQueue, ledger snapshots, memoizer | `module04/docs/module04.md` |
| Generics, phantom currency types, typed event bus | `module05/docs/module05.md` |
| Streams, netting, AML rule | `module06/docs/module06.md` |
| Java 17/21 features, lifecycle state machine, text blocks | `module07/docs/module07.md` |
| Exceptions, validation, log sanitising, ambiguous outcomes | `module08/docs/module08.md` |
| Concurrency basics, schedulers, context propagation, delivery semantics | `module09/docs/module09.md` |
| JMM, snapshots, CAS with VarHandle | `module10/docs/module10.md` |
| Locks, conditions, deadlock detection, pool sizing | `module11/docs/module11.md` |
| CompletableFuture, retry with jitter, bulkhead | `module12/docs/module12.md` |
| Virtual threads, structured concurrency, ScopedValue, admission control | `module13/docs/module13.md` |
| JVM, GC, JIT, minor units, benchmarking | `module14/docs/module14.md` |
| Coding problems P01-P17 (outbox, per-key ordering) | `module15/docs/module15.md` |
| Live-coding katas and production hardening | `module16/docs/` |
| Build It systems | `module17/docs/`, `REVOLUT_BUILD_IT_PROBLEMS.md` |
| Sealed types and pattern matching | `explanations/docs/explanations.md` |
