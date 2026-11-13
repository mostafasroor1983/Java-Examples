# Module 16 - Revolut Interview Prep

Package root: `com.revolut.interview`. Tests use JUnit 5.10.0 + AssertJ 3.24.2.

| Package | Kata | Guide |
|---|---|---|
| `bank` | account with invariants, `Money`, `AccountId`, `Bank` registry | `docs/02_bank_account_kata.md` |
| `bankTransfer` | atomic, idempotent, deadlock-free transfer | `docs/03_bank_transfer_kata.md` |
| `payment` | create/authorise/capture/refund with fraud checks and a clock | `docs/04_payment_service_kata.md` |
| `ratelimiter` | fixed window, sliding log, token bucket behind `RateLimiter` | `docs/05_rate_limiter_kata.md` |

Read `01_interview_format.md` first, then `06_concurrency_checklist.md` and `07_testing_checklist.md`; `08_system_design_talking_points.md` is for the design round.

## BAD vs GOOD (the things that fail candidates)
```java
// BAD: double for money, exceptions for expected outcomes, locking on `this`, get-then-put
double balance; if (balance >= amount) balance -= amount; else throw new RuntimeException();
// GOOD: BigDecimal value object, typed result, private lock, atomic claim
Money balance; synchronized (lock) { if (balance.compareTo(amount) < 0) throw new InsufficientFundsException(...); balance = balance.minus(amount); }
processed.putIfAbsent(requestId, placeholder);
```

## Key Takeaways
- State invariants first; design the lock/CAS around them; test them under N threads.
- Idempotency is a first-class requirement for any money-moving API.
- Lock ordering by id is the default answer to two-account atomicity in memory; sagas/outbox in distributed systems.
- Inject clocks; your rate limiter and payment timestamps become unit-testable in milliseconds.
- Typed results for expected outcomes, exceptions for programmer errors, AssertJ for readable tests.

## Cheat Sheet
| Kata | Must-have | Nice-to-have |
|---|---|---|
| Account | positive amounts, no overdraft, history, thread-safe | overdraft limit, statement paging |
| Transfer | atomic, ordered locks, idempotent, conserved total | tryLock/back-off, saga discussion |
| Payment | state machine, fraud checks, money movement, clock | partial capture, async fraud with timeout |
| Rate limiter | per-key, exact under concurrency, injected clock | distributed (Redis), 429 + Retry-After |

## 10 Interview Q&A
1. **How do you prevent deadlock when transferring between two accounts?** Acquire locks in a global order (account id) or use `tryLock` with timeout and retry; never nest in argument order.
2. **How do you make a transfer idempotent?** Client-supplied request id, atomically claimed (`putIfAbsent` / unique constraint); replay returns the stored outcome.
3. **Why return a `TransferResult` instead of throwing?** Insufficient funds is an expected business outcome; typed results force callers to handle it and keep stack traces for real bugs.
4. **Why is `Money` a record with normalised scale?** Equality and hashing must treat 1.0 and 1.00 as the same; currency safety belongs in the type.
5. **How does the payment hold work?** Authorise withdraws from the customer (hold); capture credits the merchant; refund from AUTHORISED releases the hold without touching the merchant.
6. **Fixed window vs sliding vs token bucket?** O(1)/approximate vs O(N)/exact vs O(1)/bursty-exact; choose by memory, fairness and burst tolerance.
7. **How do you test concurrency deterministically?** Latches to start together and to await completion with a timeout, exact totals, injected clocks; never sleeps.
8. **Where does the fraud check belong?** Behind an interface, composed as a list, evaluated before funds move; failures produce DECLINED with a reason.
9. **What would change for a distributed deployment?** Per-account partitioning, DB constraints for idempotency, outbox for events, sagas for cross-shard transfers.
10. **How do you handle a provider timeout during authorise?** Treat as unknown: do not approve; mark for reconciliation/manual review and query the provider by idempotency reference.

## Exercises for the reader
1. Add an overdraft facility to `Account` and keep every test green.
2. Make `TransferService` emit an event list (outbox) and prove exactly one event per completed transfer under concurrency.
3. Implement a `RedisLikeFixedWindow` using a `ConcurrentHashMap<String, Long>` with TTL semantics and compare with `FixedWindowRateLimiter`.
