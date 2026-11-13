# 06 - Concurrency checklist for live coding

Before you say "done", walk this list aloud:
1. **Shared mutable state** - where is it? Who writes it? Every write path goes through one lock / CAS / concurrent structure.
2. **Compound actions** - any get-then-put, check-then-act, read-modify-write? Replace with `compute/merge/putIfAbsent` or hold the lock across both.
3. **Lock scope** - critical sections short; no I/O, no callbacks, no `join()` inside.
4. **Lock order** - more than one lock? Global ordering (by id) or `tryLock` with back-off.
5. **Visibility** - plain fields read by other threads need `volatile`, a lock, or an atomic.
6. **Invariants under failure** - exception in the middle of a transfer leaves balances consistent?
7. **Interruption** - blocking calls handle `InterruptedException` by restoring the flag or propagating.
8. **Thread pools** - bounded queue, named threads, rejection policy, graceful shutdown.
9. **Tests** - N threads, latch to start together, latch/`await` with timeout to finish, assert exact totals; no sleeps.
10. **Say what you would use in production** - `ConcurrentHashMap`, `LongAdder`, Caffeine, Resilience4j, DB constraints, idempotency keys.

Phrases that score: "this is a check-then-act, so I'll use computeIfAbsent"; "I'm ordering locks by account id to prevent deadlock"; "I inject the clock so the test is deterministic"; "money is BigDecimal with explicit scale".
