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
