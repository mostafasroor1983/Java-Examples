# Module 10 - Java Memory Model, volatile and Atomics

Package root: `module10`. Domain: FX rate publication, balances, request stats.

## The JMM in five sentences
1. Each thread may cache values and the compiler may reorder instructions unless a **happens-before** edge forbids it.
2. Happens-before edges come from: program order within a thread, monitor unlock -> lock, volatile write -> read, `Thread.start()`, `Thread.join()`, and every `java.util.concurrent` synchroniser (latches, executors, futures, concurrent collections).
3. A data race (two unsynchronised accesses, one a write) makes the read's result unspecified: stale forever, or a half-constructed object.
4. `final` fields written in a constructor are visible to every thread once the constructor finishes, even through a race (as long as `this` does not escape).
5. `volatile` guarantees visibility and ordering, not atomicity of compound actions.

## BAD vs GOOD
```java
// BAD: plain shared field - reader may spin forever on a stale value
private FxRate current;                     void publish(FxRate r) { current = r; }
// GOOD: any of these
private volatile FxRate current;
private final AtomicReference<FxRate> current;
synchronized (lock) { current = r; }
```
```java
// BAD: double-checked locking without volatile
if (value == null) synchronized (this) { if (value == null) value = create(); }   // reader may see non-null, half-built
// GOOD
private volatile T value;  // same code, now correct (Java 5+)
// BEST for statics
private static final class Holder { static final T INSTANCE = create(); }
```
```java
// BAD: CAS on a reference is vulnerable to ABA when the same instance comes back
ref.compareAndSet(observed, next);  // succeeds after A->B->A
// GOOD
AtomicStampedReference<T> ref; ref.compareAndSet(observed, next, stamp, stamp + 1);
```

## What each test proves
| Test | Claim |
|---|---|
| `PublicationTest` | volatile/atomic/locked publication is visible; `final` + defensive copy; volatile DCL constructs once under 16 threads |
| `HappensBeforeTest` (x20) | start, join, volatile, latch, monitor edges all publish a *plain* int |
| `CasTest` | hand-rolled CAS loop exact under 8 x 50k; cap never exceeded; ABA shown and fixed; lock-free stack holds 40k pushes |
| `AtomicsDeepDiveTest` | `AtomicReference<BigDecimal>` balance never negative; `LongAccumulator`/`LongAdder`/`AtomicIntegerArray` exact; `getAndUpdate` vs `updateAndGet` |
| `HeapVsStackTest` | recursion overflow depth, pass-by-value of references, `Integer` cache trap |

## Heap vs Stack
| | Stack | Heap |
|---|---|---|
| Owner | one thread | all threads |
| Holds | frames: locals, operand stack, references | objects, arrays, class instances |
| Lifetime | method return | until unreachable (GC) |
| Failure | `StackOverflowError` | `OutOfMemoryError` |
| Thread safety | inherent | needs synchronisation |
| Size | `-Xss` (default ~512KB-1MB) | `-Xmx` |

Escape analysis lets the JIT scalar-replace objects that never escape a method, so "allocation" of short-lived
`BigDecimal`s in a loop may cost nothing. Do not assume; measure (module14).

## Key Takeaways
- Every shared mutable field needs a happens-before story: lock, volatile, atomic, or a j.u.c. structure.
- Prefer immutable objects + `AtomicReference` swap over mutable objects + locks.
- CAS loops must be short and the function pure; it may be invoked multiple times.
- `LongAdder` for hot counters, `AtomicLong` when you need the exact value on every increment, `LongAccumulator` for max/min.
- ABA is mostly a native-memory/lock-free-reuse problem; in Java GC prevents address reuse while a reference is held, but stamps are still needed for logical ABA.
- `Integer == Integer` is a bug outside -128..127.

## Cheat Sheet
| Need | Tool |
|---|---|
| stop flag | `volatile boolean` |
| counter, exact every time | `AtomicLong` |
| counter, read rarely | `LongAdder` |
| max/min | `LongAccumulator` |
| swap immutable snapshot | `AtomicReference` + `accumulateAndGet`/CAS loop |
| version + value | `AtomicStampedReference` |
| lazy singleton | holder idiom or `volatile` DCL |
| publish object | `final` fields + safe publication |

## 10 Interview Q&A
1. **What is happens-before?** A partial order on actions; if A happens-before B, A's effects are visible to B. Without it, visibility is not guaranteed.
2. **What does `volatile` guarantee?** Reads/writes go to main memory in order; a volatile write happens-before every subsequent read of that field. No atomicity for `++`.
3. **Why does DCL need `volatile`?** Without it, the reference may be published before the object's fields are written; a reader sees non-null but uninitialised state.
4. **How are `final` fields special?** The JMM guarantees their values are visible after construction even without synchronisation, provided `this` does not escape the constructor.
5. **What is CAS?** Compare-and-swap: atomically set a value only if it still equals the expected one. The basis of all lock-free algorithms; backed by a CPU instruction.
6. **Explain ABA.** A CAS sees the expected value A and succeeds even though the state went A->B->A in between. Fix with a version stamp.
7. **`AtomicLong` vs `LongAdder`?** `AtomicLong` is a single contended cell; `LongAdder` stripes across cells and sums on read, trading read cost for write throughput.
8. **Is `AtomicReference<BigDecimal>` thread-safe for balances?** Yes, if updates use CAS loops / `accumulateAndGet` with pure functions; `BigDecimal` is immutable.
9. **Where do local variables live?** On the thread's stack frame; primitives inline, objects referenced from there but allocated on the heap (modulo escape analysis).
10. **Why does `Integer a = 128, b = 128; a == b` return false?** Autoboxing caches -128..127; outside that range new objects are created and `==` compares references.

## Exercises for the reader
1. Implement a lock-free `AtomicMoneyMap` (currency -> balance) using `ConcurrentHashMap.compute` and compare with `AtomicReference<Map>` swap.
2. Add a bounded `LockFreeQueue` (Michael-Scott) and test exact totals under 8 producers/8 consumers.
3. Use `VarHandle` to implement `CasCounter` without `AtomicLong` and explain acquire/release modes.
