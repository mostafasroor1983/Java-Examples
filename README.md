# Java-Examples - module-by-module Java deep dive for senior/staff fintech interviews

A Maven multi-module repository (`com.javaexamples:java-examples-parent:1.0.0`) where every example lives in a payments domain:
payments, wallets, accounts, ledgers, FX, fees, fraud, idempotency and rate limiting. Each module is standalone (no parent link),
has JUnit tests for contracts, invariants, error paths and concurrency, and a study guide in `docs/`.

Toolchain: JDK 21, Maven 3.9+, UTF-8. Test libraries only (JUnit Jupiter 5.10.x; AssertJ 3.24.2 in module16/17). Main code is JDK-only.
Versions deliberately vary between modules to mirror a real estate of services; do not normalise them.

**Java 17 vs 20 vs 21.** Modules target Java 17 or Java 21 (see the table). There is no Java 20 build: Java 20 was a short-lived
release in which virtual threads, record patterns and switch patterns were *previews*; they became final in 21. Every module doc has a
"Java version notes" table that labels each feature as Java 17, Java 21 (final), or preview, and shows the Java 19/20 status where relevant.

```bash
mvn clean test                # everything
mvn -pl module04 test         # one module (from the root)
```

## Module index
| Module | Artifact | Java | Topic | Highlights |
|---|---|---|---|---|
| [module01](module01/docs/module01.md) | module01-oop-clean-design | 21 (preview) | OOP, SOLID | bad/good per principle, Wallet, fee strategies with PREMIUM discount |
| [module02](module02/docs/module02.md) | module02-equals-hashcode-immutability | 17 | equals/hashCode, immutability | mutable-key trap, Money/Email/CustomerId, PaymentLedger HashMap bug, ConfigHolder |
| [module03](module03/docs/module03.md) | module03-collections-hashmap | 17 | Collections | HashMap internals + treeify, FxPairMap, frequency analyzer, LRU v1/v2/v3, ledger |
| [module04](module04/docs/module04.md) | module04-concurrent-collections | 21 | Concurrent collections | CHM atomic ops, idempotency cache, queues, COW, lost updates, SlidingWindowRateLimiter |
| [module05](module05/docs/module05.md) | module05-generics | 17 | Generics | bounds, PECS, erasure, `Repository<T,ID>`, `Result<T,E>` |
| [module06](module06/docs/module06.md) | module06-streams-functional-programming | 17 | Streams | collectors, laziness, parallel pitfalls, RiskReport, AccountSummary, FxConversionResult |
| [module07](module07/docs/module07.md) | module07-modern-java-17 | 17 props / 21 compiler | Modern Java | records, sealed PaymentResult, switch patterns, text blocks, Java 8 vs 17 |
| [module08](module08/docs/module08.md) | module08-exceptions-error-handling | 21 | Exceptions | hierarchy, wrapping, try-with-resources, CircuitBreaker, PaymentErrorResponse |
| [module09](module09/docs/module09.md) | module09-concurrency-fundamentals | 17 | Concurrency basics | threads, synchronized/volatile/atomics, executors, latch/barrier/semaphore, producer-consumer |
| [module10](module10/docs/module10.md) | module10-java-memory-model-volatile-atomic | 21 | JMM | happens-before, CAS and ABA, atomics, heap vs stack |
| [module11](module11/docs/module11.md) | module11-locks-deadlocks-thread-pools | 17 | Locks and pools | ReentrantLock/RW/Stamped, deadlock + fixes, livelock, ThreadPoolExecutor sizing |
| [module12](module12/docs/module12.md) | module12-completablefuture-async | 21 | CompletableFuture | composition, errors, timeouts, PriceService, OrderFulfilment, PaymentWorkflow |
| [module13](module13/docs/module13.md) | module13-java21-modern-concurrency | 21 (preview) | Java 21 concurrency | virtual threads, structured concurrency, ScopedValue, pinning |
| [module14](module14/docs/module14.md) | module14-jvm-internals-performance | 21 | JVM internals | class loading, JIT, string pool, GC pressure, boxing, false sharing, contention |
| [module15](module15/docs/module15.md) | module15-senior-backend-coding-problems | 17 | Coding problems | P01-P15: counter, balance, transfer, state machine, idempotency, dedup, limiter, LRU, ... |
| [module16](module16/docs/module16.md) | module16-revolut-interview-prep | 17 + AssertJ | Revolut live coding | `com.revolut.interview.{bank,bankTransfer,payment,ratelimiter}`, guides 01-08 |
| [module17](module17/docs/README.md) | module17-revolut-build-it | 17 + AssertJ | Revolut Build It | `com.revolut.buildit.*`: load balancer, cart, transfer, subscription, FX, shortener, limiter, ledger |
| [module18](module18/docs/module18.md) | module18-effective-java | 21 (17 + labelled 21 features) | Effective Java 3rd ed. | all 90 items: one class + one test per item, chapter docs, Java 17/21 updates |
| [explanations](explanations/docs/explanations.md) | explanations-sealed-pattern-matching | 21 | Sealed + patterns | Success/Failure/Pending, every pattern-matching form |

Cross-cutting documents: [OVERALL_CHEATSHEET.md](OVERALL_CHEATSHEET.md), [REVOLUT_BUILD_IT_PROBLEMS.md](REVOLUT_BUILD_IT_PROBLEMS.md).

## What every module doc contains
Each `docs/moduleNN.md` starts with **Overview, Learning objectives, Prerequisites, Java version notes**, keeps the original
BAD/GOOD, cheat sheet and Q&A sections, and ends with a **Production deep dive**. The deep dive covers why each example matters in a bank or fintech, expected behaviour,
edge cases and failure scenarios, trade-offs, Mermaid diagrams, **in-memory vs production guarantees**, common pitfalls and a checklist.

| Module | Production-grade additions |
|---|---|
| 01 | `ddd` maker-checker approval aggregate (separation of duties, domain events); `hexagonal` transfer use case (ports, optimistic locking, idempotency) |
| 02 | `sensitive.CardNumber` (Luhn, PCI-safe `toString`); `defensive.SignedSettlementBatch` (arrays in records); `allocation.MoneyAllocator` (no lost cents) |
| 03 | `reconciliation.StatementReconciler` (O(n+m) breaks report); `history.BalanceHistory` (as-of balances, back-valuation) |
| 04 | `delay.PaymentRetryScheduler` (`DelayQueue`); `ledger.InMemoryDoubleEntryLedger` (ordered locks, consistent snapshots); `cache.FxRateCache` (stampede-proof memoizer) |
| 05 | `currency.Amount<C>`/`FxRate<F,T>` (currency in the type); `events.TypedEventBus` (type tokens, PECS, failure isolation) |
| 06 | `settlement.NettingEngine` (multilateral netting); `aml.StructuringDetector` (AML rule) |
| 07 | `lifecycle` sealed transaction state machine (Java 21 vs Java 17, proven equivalent); `messages.CreditTransferMessage` (text blocks + XML injection) |
| 08 | `validation` accumulating validator; `redaction.LogSanitizer`; `ambiguity.AmbiguousOutcomeHandler` (timeout is not failure) |
| 09 | `scheduling.ResilientScheduler` (silent death of periodic tasks); `context` propagation and leak prevention |
| 10 | `snapshot.QuoteBook` vs `TornQuote` (multi-field consistency); `lockfree.CreditLimitReserver` (`VarHandle` CAS) |
| 11 | `conditions.FundingAwaitAccount` (`Condition` with deadlines); `diagnostics.DeadlockDetector` (`ThreadMXBean`) |
| 12 | `retry.AsyncRetry` (backoff + full jitter + deadline); `bulkhead.AsyncBulkhead` |
| 13 | `throttling.BoundedVirtualFanOut` (virtual threads + semaphore admission control) |
| 14 | `money.MinorUnitFees` (long vs BigDecimal, property-tested); `benchmark.MiniBenchmark`; `allocation.AllocationProbe` |
| 15 | `P16TransactionalOutbox` (at-least-once + idempotent consumer); `P17PerKeyOrderedExecutor` |
| 16 | randomized transfer stress test, payment race tests, `docs/09_production_hardening.md`, deeper dives in guides 02-05/07 |
| 17 | `Ledger.reverse` (immutable corrections), deeper dives in every approach note |
| explanations | `OutcomeResponder` (outcome -> API response + action; fraud never revealed) |

## Conventions
- Money is `BigDecimal` with explicit scale; `double` appears only in deliberately "bad" examples.
- IDs are record value objects; inputs are validated at construction.
- `bad/` packages compile and demonstrate the flaw; `good/` fixes it. Alternative solutions are kept side by side (`X`, `X2`, `v2`).
- Concurrency tests use latches and exact totals, never long sleeps. Clocks are injected.
- No secrets, no unsafe deserialisation, no frameworks.

## Suggested study order
01 -> 02 -> 03 -> 05 -> 06 -> 07 -> explanations -> 08 -> 09 -> 10 -> 04 -> 11 -> 12 -> 13 -> 14 -> 15 -> 16 -> 17 -> 18.
