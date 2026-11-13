# 02 - Kata: Bank account (`com.revolut.interview.bank`)

Prompt: "Implement a bank account supporting deposits, withdrawals and balance; it must be safe to use from multiple threads."

Invariants to state out loud
- Balance never negative.
- Amounts positive; single currency per account.
- Every mutation is recorded (statement).
- Thread-safe without exposing the lock.

Design
- `Money` record: scale-normalised `BigDecimal` + `Currency`; `CurrencyMismatchException`.
- `AccountId` record: validated.
- `Account`: private lock object, `deposit/withdraw/balance/transactions`; returns `List.copyOf`.
- `Bank`: `ConcurrentHashMap<AccountId, Account>`, `putIfAbsent` on open, `Optional` lookup.

Tests that interviewers want to see (`AccountTest`)
- happy path, overdraft rejected with message, invalid amounts, currency mismatch
- registry duplicate / unknown id
- 10 threads x 1000 deposits of 0.01 => exactly 100.00 and 10 000 transactions

Follow-up questions
- "Why `synchronized` and not `AtomicReference<Money>`?" - both work; CAS loop avoids blocking but the statement list needs the same atomicity, so one lock is simplest.
- "How would you persist?" - append-only ledger table, balance derived or maintained with optimistic version column.
- "Overdraft facility?" - add `overdraftLimit` to the invariant check (module15 P02).
