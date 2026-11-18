# 07 - Testing checklist (AssertJ + JUnit 5)

Structure
- One test class per unit, `@BeforeEach` for fixtures, descriptive method names that read as requirements.
- Arrange / act / assert blocks; one behaviour per test, several assertions are fine when they describe one outcome.

AssertJ idioms used in this module
```java
assertThat(acc.balance()).isEqualTo(Money.of("60.00", "GBP"));
assertThat(acc.transactions()).hasSize(2).extracting(Transaction::kind).containsExactly(DEPOSIT, WITHDRAWAL);
assertThatThrownBy(() -> acc.withdraw(big)).isInstanceOf(InsufficientFundsException.class).hasMessageContaining("requested");
assertThat(result).extracting(Payment::status, Payment::reason).containsExactly(DECLINED, "amount too large");
assertThat(done.await(10, SECONDS)).as("no deadlock").isTrue();
```

Coverage that interviewers look for
- Happy path, each validation branch, each state transition (legal and illegal).
- Idempotency: repeat the same call, assert no double effect.
- Concurrency: exact totals under N threads; opposing operations for deadlock freedom; bounded waits.
- Determinism: injected clocks (`Supplier<Instant>`, `LongSupplier`), no `Thread.sleep`.
- Immutability leaks: returned collections are unmodifiable.

Anti-patterns to avoid live
- Asserting on `toString()`; testing private methods; sleeping to "let threads finish"; one giant test.

## Beyond unit tests (what to mention for senior roles)
| Test type | Example in this repo | What it catches |
|---|---|---|
| Concurrency / stress | `TransferServiceStressTest`, module04 `InMemoryDoubleEntryLedgerTest` | lost updates, deadlocks, broken invariants under interleaving |
| Property-based (seeded random) | module02 `MoneyAllocatorTest`, module14 `MinorUnitFeesTest` | edge cases you did not think of; keep the seed for reproducibility |
| Equivalence / differential | module07 Java 17 vs 21 lifecycle | regressions when rewriting or optimising |
| Failure injection | module15 `P16Test` (crash after publish), module08 ambiguity tests | duplicate delivery, lost events, timeouts |
| Integration | (not in this repo) Testcontainers with Postgres/Kafka | real isolation levels, constraints, broker semantics |
| Performance | module14 `MiniBenchmark` (and JMH) | regressions in hot paths; never assert timings in unit tests |

Rules for concurrency tests: start all threads on a latch, bound every wait with a timeout, assert *invariants*
(totals, no negative, exactly-once), use `@RepeatedTest` to shake out rare interleavings, and never use `Thread.sleep` to
"let threads finish".
