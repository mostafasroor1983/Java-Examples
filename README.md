# Java-Examples - module-by-module Java deep dive for senior/staff fintech interviews

A Maven multi-module repository (`com.javaexamples:java-examples-parent:1.0.0`) where every example lives in a payments domain:
payments, wallets, accounts, ledgers, FX, fees, fraud, idempotency and rate limiting. Each module is standalone (no parent link),
has JUnit tests for contracts, invariants, error paths and concurrency, and a study guide in `docs/`.

Toolchain: JDK 21, Maven 3.9+, UTF-8. Test libraries only (JUnit Jupiter 5.10.x; AssertJ 3.24.2 in module16/17). Main code is JDK-only.
Versions deliberately vary between modules to mirror a real estate of services; do not normalise them.

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
| [explanations](explanations/docs/explanations.md) | explanations-sealed-pattern-matching | 21 | Sealed + patterns | Success/Failure/Pending, every pattern-matching form |

Cross-cutting documents: [OVERALL_CHEATSHEET.md](OVERALL_CHEATSHEET.md), [REVOLUT_BUILD_IT_PROBLEMS.md](REVOLUT_BUILD_IT_PROBLEMS.md).

## Conventions
- Money is `BigDecimal` with explicit scale; `double` appears only in deliberately "bad" examples.
- IDs are record value objects; inputs are validated at construction.
- `bad/` packages compile and demonstrate the flaw; `good/` fixes it. Alternative solutions are kept side by side (`X`, `X2`, `v2`).
- Concurrency tests use latches and exact totals, never long sleeps. Clocks are injected.
- No secrets, no unsafe deserialisation, no frameworks.

## Suggested study order
01 -> 02 -> 03 -> 05 -> 06 -> 07 -> explanations -> 08 -> 09 -> 10 -> 04 -> 11 -> 12 -> 13 -> 14 -> 15 -> 16 -> 17.
