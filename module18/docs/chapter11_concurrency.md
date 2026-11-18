# Chapter 11 - Concurrency (items 78-84)

Package `module18.ch11`, tests in `Chapter11Test`. Theme: correct sharing, minimal locking, high-level utilities, and
documented thread safety. This chapter has the biggest Java 21 update: **virtual threads** (item 80). Deep dives:
modules 09-13.

| Item | Rule | Java 21 note |
|---|---|---|
| 78 | synchronise reads AND writes of shared mutable data | unchanged (JMM) |
| 79 | no alien calls under locks; keep critical sections small | pinning: avoid blocking inside `synchronized` on JDK 21-23 |
| 80 | executors and tasks, not raw threads | `newVirtualThreadPerTaskExecutor()`; `ExecutorService` is AutoCloseable (19) |
| 81 | j.u.c. utilities instead of wait/notify | structured concurrency in preview (21) |
| 82 | document the thread-safety level | - |
| 83 | lazy init only when needed: holder class / double-check | `ScopedValue` (preview) for context, not lazy init |
| 84 | never rely on the scheduler | virtual threads make busy-waiting even worse (carrier starvation) |

---

## Item 78 - Synchronize access to shared mutable data
**In short.** Synchronisation provides mutual exclusion and visibility. Without it a stop flag may never be seen and `++` loses
updates. Readers need it as much as writers. Best: do not share mutable data.
**Example.** `SettlementWorker` stops via a `volatile` flag; `SafeSerial` (AtomicLong) gives 80,000 unique serials under 8 threads;
`UnsafeSerial` (`next++`) would not.
**Proof.** `item78_volatileStopFlagAndAtomicCounter`.

## Item 79 - Avoid excessive synchronization
**In short.** Never call alien methods (listeners, callbacks, overridable methods) while holding a lock: they can re-enter
(ConcurrentModificationException) or deadlock. Keep critical sections short (open calls).
**Example.** `ObservableLedger.post` updates the balance under a private lock, then notifies listeners outside it.
`CopyOnWriteArrayList` lets a listener unsubscribe itself during notification.
**Java 17/21.** With virtual threads on JDK 21-23, blocking inside `synchronized` pins the carrier thread (fixed in JDK 24, JEP 491).
**Proof.** `item79_openCallsAllowSelfUnsubscribe`.

## Item 80 - Prefer executors, tasks, and streams to threads
**In short.** Separate the task (Runnable/Callable) from the execution policy (executor). Executors give pooling, results,
exceptions via `Future`, timeouts and shutdown.
**Example.** `fetchBalancesPool` (Java 17: fixed pool + `invokeAll` + shutdown) and `fetchBalancesVirtual` (**Java 21**: one virtual
thread per blocking lookup, try-with-resources closes and waits). Both return identical results, and 2,000 blocking lookups run
cheaply on virtual threads.
**Java 17/21.** Virtual threads: preview 19/20, **final 21** (JEP 444). Don't pool them; limit downstream concurrency with a
`Semaphore` (module13).
**Proof.** `item80_poolAndVirtualThreadsGiveSameResults`.

## Item 81 - Prefer concurrency utilities to wait and notify
**In short.** Use concurrent collections, synchronizers (`CountDownLatch`, `Semaphore`, `CyclicBarrier`, `Phaser`) and
BlockingQueues. If you must use wait/notify: always wait in a `while` loop, and prefer `notifyAll`.
**Example.** `canonical(code)` uses `ConcurrentHashMap.putIfAbsent` as an atomic intern; `timeConcurrentSettlement` uses
ready/start/done latches.
**Proof.** `item81_concurrencyUtilities`.

## Item 82 - Document thread safety
**In short.** State the level: immutable, unconditionally thread-safe, conditionally thread-safe (which lock to hold), not
thread-safe, or thread-hostile. Use a private lock object rather than `this`.
**Example.** `ThreadSafety` annotations (`@ThreadSafe`, `@NotThreadSafe`, `@Immutable`, `@GuardedBy`) on `AccountStatement`
(private lock, 8 threads x 1,000 adds = 8,000), `BatchBuilder` and `Rate`.
**Proof.** `item82_documentedThreadSafeClassIsThreadSafe`.

## Item 83 - Use lazy initialization judiciously
**In short.** Initialise eagerly by default. For static fields, the holder-class idiom; for instance fields, double-check with a
volatile field and a local variable.
**Example.** `bicDirectory()` loads once via `BicDirectoryHolder`; `FraudModel.weights()` loads once under 16 concurrent callers.
**Pitfalls.** Double-checked locking without `volatile` can publish a half-built object (module10).
**Proof.** `item83_lazyInitialisedExactlyOnce`.

## Item 84 - Don't depend on the thread scheduler
**In short.** Correctness must not depend on priorities, `Thread.yield` or timing. Do not busy-wait; block on a synchronizer. Keep
the number of runnable threads small.
**Example.** `SpinningLatch` burns a core while waiting; `awaitSettlementFiles` blocks on a `CountDownLatch` with a timeout.
**Java 17/21.** Busy-waiting on virtual threads can monopolise carrier threads; blocking primitives unmount properly.
**Proof.** `item84_blockInsteadOfSpinning`.

---
**Say it in an interview.** "Volatile for flags, atomics for counters, locks for compound state; no alien calls under a lock;
executors (virtual threads on 21 for I/O) instead of raw threads; latches and concurrent maps instead of wait/notify; thread
safety documented; holder idiom for lazy statics; never busy-wait."
