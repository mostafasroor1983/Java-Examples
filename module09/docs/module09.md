# Module 09 - Concurrency Fundamentals

Package root: `module09`. Domain: settlement workers, balance counters, account transfers, provider call limits.

## Map
| Package | What it proves |
|---|---|
| `threads.ThreadBasics` | start/join, naming, cooperative interruption, `InterruptedException` clears the flag, uncaught handlers, daemon threads |
| `sync.Counters` | `count++` loses updates; `volatile` does not fix it; `synchronized`, `AtomicLong`, `LongAdder` are exact under 8 threads x 50k |
| `sync.VolatileFlag` | the legitimate use of `volatile`: a stop flag |
| `sync.Account` | private lock object, lock ordering for deadlock-free transfers, money conserved under 8 threads |
| `executors.ExecutorDemos` | `invokeAll` order, exceptions via `Future.get`, timeout + `cancel(true)`, graceful shutdown |
| `coordination.Coordination` | `CountDownLatch` start/done gates, `CyclicBarrier` phases with barrier action, `Semaphore` bounded concurrency |
| `producerconsumer.*` | `wait/notifyAll` bounded buffer done right, the broken `if`/`notify` version, poison-pill pipeline with exact totals |

## BAD vs GOOD
```java
// BAD: visibility is not atomicity
private volatile long count; public void increment() { count++; }
// GOOD
private final AtomicLong count = new AtomicLong(); public void increment() { count.incrementAndGet(); }
```
```java
// BAD: if + notify -> stolen wake-ups, spurious wake-ups, overfilled buffer
if (items.isEmpty()) wait(); return items.pollFirst();
// GOOD: while + notifyAll
while (items.isEmpty()) wait(); T item = items.pollFirst(); notifyAll(); return item;
```
```java
// BAD: lock on `this` or a String literal: anyone can lock it, deadlock you, or notify you
synchronized (this) { ... }     synchronized ("LOCK") { ... }
// GOOD
private final Object lock = new Object(); synchronized (lock) { ... }
```
```java
// BAD: two accounts locked in caller order -> A->B and B->A deadlock
synchronized (from.lock) { synchronized (to.lock) { ... } }
// GOOD: global order (by id)
Account first = from.id.compareTo(to.id) < 0 ? from : to; ...
```

## Thread lifecycle in one paragraph
`new` -> `start()` -> RUNNABLE; blocks on monitors (BLOCKED), `wait/join/park` (WAITING / TIMED_WAITING); TERMINATED when `run()` returns or throws.
An uncaught exception kills only that thread and goes to its `UncaughtExceptionHandler`. `interrupt()` sets a flag;
blocking methods throw `InterruptedException` and clear it; CPU loops must poll `isInterrupted()`.

## Executors you should name
| Factory | Queue | Use |
|---|---|---|
| `newFixedThreadPool(n)` | unbounded `LinkedBlockingQueue` | CPU-bound, known parallelism (beware unbounded queue) |
| `newCachedThreadPool()` | `SynchronousQueue`, unbounded threads | short bursty tasks; dangerous under load |
| `newSingleThreadExecutor()` | unbounded | serialise work |
| `newScheduledThreadPool(n)` | delay queue | periodic jobs |
| `new ThreadPoolExecutor(...)` | your choice | production: bounded queue + rejection policy (module11) |

## Key Takeaways
- `synchronized` gives atomicity + visibility; `volatile` gives visibility only; atomics give lock-free atomicity for single variables.
- Always `wait()` in a `while` loop and prefer `notifyAll()`.
- Lock on private final objects; order locks globally when you need more than one.
- Submit `Callable`s and call `get()` so exceptions do not vanish; `execute(Runnable)` sends them to the thread handler.
- Shutdown: `shutdown()` -> `awaitTermination()` -> `shutdownNow()` -> `awaitTermination()`.
- Tests: latches for start/finish, exact totals, bounded waits; never `Thread.sleep` as synchronisation.

## Cheat Sheet
| Primitive | One-shot? | Purpose |
|---|---|---|
| `CountDownLatch` | yes | wait for N events |
| `CyclicBarrier` | reusable | N threads meet, optional action |
| `Semaphore` | n/a | limit permits / admission control |
| `Phaser` | reusable, dynamic | advanced phased work |
| `wait/notifyAll` | n/a | condition waiting on an intrinsic lock |
| `Condition` | n/a | same with `ReentrantLock` (module11) |

## 10 Interview Q&A
1. **Why does `volatile long count; count++` lose updates?** `++` is read, add, write; two threads can read the same value. Volatile only guarantees each read sees the latest write.
2. **What does `synchronized` guarantee?** Mutual exclusion on the monitor and a happens-before edge between unlock and subsequent lock, so writes are visible.
3. **Why `while` around `wait()`?** Spurious wake-ups are allowed by the spec and `notifyAll` wakes threads whose condition may still be false.
4. **`notify` vs `notifyAll`?** `notify` wakes one arbitrary waiter; if it is the wrong kind (a producer when a consumer was needed) the signal is lost. `notifyAll` is safe.
5. **How do you stop a thread?** Interrupt it and have it check `isInterrupted()` / handle `InterruptedException`. `Thread.stop` is removed.
6. **What happens to an exception thrown inside `submit()`?** Captured in the `Future`; rethrown wrapped in `ExecutionException` on `get()`.
7. **Difference between `shutdown` and `shutdownNow`?** `shutdown` stops accepting and drains the queue; `shutdownNow` also interrupts workers and returns queued tasks.
8. **`CountDownLatch` vs `CyclicBarrier`?** Latch counts events and cannot reset; barrier waits for N threads and resets for the next cycle.
9. **Why is `newCachedThreadPool` risky?** Unbounded thread creation under load; prefer a bounded `ThreadPoolExecutor`.
10. **Is `Thread.sleep` a synchronisation tool?** No. It gives no happens-before guarantees and makes tests slow and flaky; use latches/barriers.

## Exercises for the reader
1. Replace `WaitNotifyQueue` internals with `ReentrantLock` + two `Condition`s (`notFull`, `notEmpty`) and keep all tests green.
2. Write a `Phaser`-based version of `phasedSettlement` where workers can leave after phase 1.
3. Add a `Semaphore`-based `RateLimitedProviderClient` with `tryAcquire(timeout)` and test both admit and shed paths.
