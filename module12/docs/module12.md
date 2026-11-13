# Module 12 - CompletableFuture and Async Programming

Package root: `module12`. Domain: FX price aggregation, card order fulfilment, async payment workflow.

## The vocabulary
| Want | Method | Analogy |
|---|---|---|
| transform value | `thenApply` | `map` |
| chain async call | `thenCompose` | `flatMap` |
| combine two | `thenCombine` | `zip` |
| wait for all | `allOf(...).thenApply(v -> join each)` | fan-in |
| first to finish | `anyOf` | race |
| recover | `exceptionally`, `exceptionallyCompose` | catch |
| map both paths | `handle` | fold |
| observe | `whenComplete` | finally/log |
| deadline | `orTimeout`, `completeOnTimeout` | timeout |
| start work | `supplyAsync(s, executor)` | submit |

## BAD vs GOOD
```java
// BAD: nested future
CompletableFuture<CompletableFuture<BigDecimal>> f = accountId.thenApply(this::balanceOf);
// GOOD
CompletableFuture<BigDecimal> f = accountId.thenCompose(this::balanceOf);
```
```java
// BAD: blocking I/O in the common pool (shared by every parallel stream in the JVM)
CompletableFuture.supplyAsync(() -> httpClient.call());
// GOOD
CompletableFuture.supplyAsync(() -> httpClient.call(), ioPool);
```
```java
// BAD: join() inside a stage to "wait" for another future - blocks a pool thread, risks deadlock
.thenApply(v -> other.join())
// GOOD
.thenCombine(other, (v, o) -> ...)   or   .thenCompose(v -> other)
```
```java
// BAD: one failing venue fails the whole aggregate
allOf(a, b, c).thenApply(...)        // if b fails, join() throws
// GOOD: per-branch recovery before fan-in
venue.orTimeout(...).thenApply(Quote::new).exceptionally(ex -> null)   then filter nulls
```

## Threading rules (proven in `AsyncExecutorsTest`)
- `supplyAsync(s)` runs on `ForkJoinPool.commonPool()`; with an executor argument, on that executor.
- `thenApply(f)` runs on whichever thread completes the previous stage, or on the caller if already complete.
- `thenApplyAsync(f, exec)` always hops to `exec`. Use it to leave an I/O pool before CPU work.
- Timeouts (`orTimeout`) are scheduled on a single shared delayer thread: never put work in their callbacks.

## Exceptions
- A throwing stage completes exceptionally; downstream `thenApply` stages are **skipped** until an `exceptionally`/`handle`.
- `join()` throws `CompletionException`, `get()` throws `ExecutionException`; both wrap the cause once. `ErrorHandling.unwrap` walks it.
- `whenComplete` cannot change the outcome; `handle` can.
- `exceptionallyCompose` lets you recover with another async call (secondary provider, retry).

## The three services
- **PriceService**: N venues in parallel, per-venue timeout, failed venues reported, best = min rate, aggregate fails only if nobody answers.
- **OrderFulfilment**: parallel (customer, KYC, reservation) then sequential (fee, dispatch); KYC failure compensates by releasing stock; batch returns per-order outcomes with `handle`.
- **PaymentWorkflow**: fraud score with timeout -> `-1` -> MANUAL_REVIEW; async retry via recursive `exceptionallyCompose`; notification is fire-and-forget with failures logged.

## Key Takeaways
- Pass an executor to every `*Async` call that might block; reserve the common pool for pure CPU work.
- Recover per branch before `allOf` so one failure does not poison the aggregate.
- Always put a timeout on anything that talks to the network, and cancel the source on timeout.
- Never block (`join/get`) inside a stage running on the pool that would complete it.
- Test with completed/failed/never-completing futures and injected executors; no sleeps.
- For straight-line I/O code in Java 21, virtual threads (module13) are often clearer than CF chains.

## Cheat Sheet
| Scenario | Snippet |
|---|---|
| fallback value | `f.exceptionally(ex -> fallback)` |
| fallback async | `f.exceptionallyCompose(ex -> other())` |
| timeout fail | `f.orTimeout(2, SECONDS)` |
| timeout default | `f.completeOnTimeout(v, 2, SECONDS)` |
| all results | `allOf(fs).thenApply(v -> fs.stream().map(CF::join).toList())` |
| already done | `completedFuture(v)` / `failedFuture(ex)` |
| hop thread | `thenApplyAsync(f, executor)` |

## 10 Interview Q&A
1. **`thenApply` vs `thenCompose`?** `thenApply` maps to a value; `thenCompose` maps to a future and flattens. Use `thenCompose` for dependent async calls.
2. **Where does `thenApply` run?** On the thread completing the previous stage, or synchronously on the caller if that stage is already complete. `thenApplyAsync` forces a pool.
3. **Why avoid the common pool for I/O?** It is sized for CPU work and shared JVM-wide; blocking threads there stalls parallel streams and other async code.
4. **How does `allOf` return results?** It returns `CompletableFuture<Void>`; collect by `join()`-ing the inputs in a `thenApply`, which is safe because they are complete.
5. **What happens to stages after an exception?** They are skipped until a recovery stage (`exceptionally`, `handle`, `whenComplete` observes but does not recover).
6. **`join` vs `get`?** `join` is unchecked (`CompletionException`); `get` is checked (`ExecutionException`, `InterruptedException`) and supports a timeout.
7. **How do you implement timeout?** `orTimeout` to fail, `completeOnTimeout` to default; cancel the slow source in the recovery stage.
8. **How do you retry asynchronously?** Recurse through `exceptionallyCompose` with a counter; never loop with `join`.
9. **Is `CompletableFuture` cancellation cooperative?** `cancel()` completes the future exceptionally but does not interrupt the running task; the task must check or be on a cancellable primitive.
10. **CompletableFuture vs virtual threads?** CF composes asynchronous stages without blocking threads; virtual threads make blocking cheap so plain sequential code scales. Prefer the simpler one that meets the latency goal.

## Exercises for the reader
1. Add a per-venue circuit breaker (module08) to `PriceService` so a venue that failed 3 times is skipped for 30 s.
2. Give `OrderFulfilment.fulfil` an overall deadline with `orTimeout` and compensate the reservation on timeout.
3. Replace `PaymentWorkflow.retry` with exponential backoff using `CompletableFuture.delayedExecutor`.
