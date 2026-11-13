# Module 11 - Locks, Deadlocks and Thread Pools

Package root: `module11`. Domain: account locks, FX cache, reserved balances, settlement workers.

## Lock selection
| Lock | Use when | Caveats |
|---|---|---|
| `synchronized` | simple critical sections, one condition | no timeout, no interruption, no fairness |
| `ReentrantLock` | `tryLock(timeout)`, `lockInterruptibly`, fairness, multiple `Condition`s | must `unlock()` in `finally` |
| `ReentrantReadWriteLock` | read-mostly shared state (FX rate cache) | writer starvation (use fair mode), no read->write upgrade, downgrade OK |
| `StampedLock` | very read-heavy with cheap validation (optimistic reads) | not reentrant, no conditions, never hold across blocking calls |

## BAD vs GOOD
```java
// BAD: lock order follows argument order -> A->B and B->A deadlock
synchronized (from) { synchronized (to) { ... } }
// GOOD 1: global ordering
Account first = from.id.compareTo(to.id) < 0 ? from : to; first.lock.lock(); second.lock.lock(); ...
// GOOD 2: tryLock with timeout + jittered back-off, release everything on failure
if (from.lock.tryLock(5, MILLISECONDS)) { try { if (to.lock.tryLock(5, MILLISECONDS)) { ... } } finally { from.lock.unlock(); } }
```
```java
// BAD: unlock outside finally - an exception leaks the lock forever
lock.lock(); balance = balance.add(amount); lock.unlock();
// GOOD
lock.lock(); try { ... } finally { lock.unlock(); }
```
```java
// BAD: hidden unbounded queue, OOM under load
Executors.newFixedThreadPool(16);
// GOOD
new ThreadPoolExecutor(core, max, 60, SECONDS, new ArrayBlockingQueue<>(1000), namedThreads("settle"), new AbortPolicy());
```

## Deadlock: the four conditions and how we break one
Mutual exclusion, hold-and-wait, no pre-emption, circular wait. Lock ordering removes circular wait;
`tryLock` with release removes hold-and-wait. `DeadlockDemo` forces the bad interleaving with latches and proves
detection via `ThreadMXBean.findDeadlockedThreads()` (what `jstack` prints as "Found one Java-level deadlock").
Intrinsic locks cannot be interrupted, so the demo threads are daemons.

## Livelock
Threads keep running but make no progress because they react to each other symmetrically
(`LivelockDemo.politeWorkersBad`). Fix with an asymmetric tie-break (lower id wins) or randomised back-off.

## ThreadPoolExecutor decision flow
```
submit -> poolSize < core ? start thread
       -> queue.offer() succeeds ? enqueue
       -> poolSize < max ? start thread          <- only when the queue is FULL
       -> reject (handler)
```
Proven by `ThreadPoolTest.maxThreadsOnlyStartWhenQueueIsFull`: core 1, max 3, queue 2 -> pool stays at 1 until
the 4th task, then grows to 3, then rejects.

### Rejection policies
| Policy | Effect | Payments verdict |
|---|---|---|
| `AbortPolicy` | throw `RejectedExecutionException` | good: explicit load shedding, map to 503 |
| `CallerRunsPolicy` | caller executes the task | good for batch back-pressure; dangerous on request threads |
| `DiscardPolicy` / `DiscardOldestPolicy` | silent loss | never for money |
| custom | count/metric/dead-letter | good when paired with alerting |

### Sizing
- CPU-bound: `cores + 1`.
- I/O-bound: `cores * utilisation * (1 + wait/compute)`. 8 cores, 90 ms waiting per 10 ms compute -> 80 threads.
- Separate pools per dependency (bulkheads) so a slow provider cannot starve settlement.
- Java 21: virtual threads remove most of this arithmetic for I/O (module13).

### Shutdown
`shutdown()` -> `awaitTermination(t)` -> `shutdownNow()` (interrupts) -> `awaitTermination(t)`. Tasks must honour interruption.

## Key Takeaways
- Pick the weakest lock that works; measure before reaching for `StampedLock`.
- Every lock acquisition has a matching `unlock()` in `finally`.
- Never hold a lock while calling out (network, DB, another service) - that is how deadlocks and thread starvation start.
- Impose a global lock order or use `tryLock`; never both styles on the same locks.
- Always configure pools explicitly: bounded queue, named threads, chosen rejection policy, metrics.
- Prefer immutable snapshots + `AtomicReference` over `ReadWriteLock` for small read-mostly state.

## Cheat Sheet
| Symptom | Likely cause | Tool |
|---|---|---|
| threads BLOCKED forever | deadlock | `jstack`, `ThreadMXBean.findDeadlockedThreads` |
| 100% CPU, no progress | livelock / spin | thread dump shows RUNNABLE in the same frames |
| latency grows, queue grows | pool too small or unbounded queue | `ThreadPoolExecutor.getQueue().size()` metrics |
| `RejectedExecutionException` | pool saturated or shut down | check policy, add bulkhead, shed load |
| `IllegalMonitorStateException` | unlock without lock / wrong thread | review `finally` blocks |

## 10 Interview Q&A
1. **`synchronized` vs `ReentrantLock`?** Same mutual exclusion and memory semantics; `ReentrantLock` adds tryLock/timeouts/interruptibility/fairness/multiple conditions at the cost of explicit unlock.
2. **What is lock reentrancy?** The owning thread can re-acquire the lock; a hold count tracks nesting. Both `synchronized` and `ReentrantLock` are reentrant; `StampedLock` is not.
3. **When is `ReadWriteLock` slower than a plain lock?** Short critical sections with few readers: the bookkeeping outweighs the parallelism. Measure.
4. **Can you upgrade a read lock to a write lock?** Not with `ReentrantReadWriteLock` (deadlock). `StampedLock.tryConvertToWriteLock` can.
5. **How do you detect a deadlock in production?** Thread dump (`jstack`, `jcmd Thread.print`) or `ThreadMXBean.findDeadlockedThreads()` from a health check.
6. **Four conditions for deadlock?** Mutual exclusion, hold and wait, no pre-emption, circular wait. Break any one.
7. **Why does `newFixedThreadPool` risk OOM?** Its `LinkedBlockingQueue` is unbounded; under sustained overload tasks pile up until memory runs out.
8. **When do max threads get created?** Only when the queue rejects the offer, i.e. a bounded queue is full. With an unbounded queue `max` is meaningless.
9. **Which rejection policy for a payment API?** `AbortPolicy` mapped to HTTP 503 with `Retry-After`; never discard.
10. **How do you size an I/O pool?** `cores * utilisation * (1 + wait/compute)`; or use virtual threads and size by downstream capacity instead.

## Exercises for the reader
1. Add fairness to `ReadWriteFxCache` and measure writer wait time under 16 readers.
2. Implement a `Bulkhead` wrapper: one bounded pool per provider, exposing queue depth and rejections as metrics.
3. Convert `DeadlockFixes.transferTryLock` to use `lockInterruptibly` and prove that `shutdownNow` unblocks a stuck transfer.
