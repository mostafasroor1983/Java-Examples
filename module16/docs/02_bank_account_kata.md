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

## Deeper dive: what a strong candidate adds
**Edge cases to test or at least name:** zero and negative amounts, sub-penny amounts (`0.001 GBP` must be rejected, not rounded
silently), currency mismatch, very large amounts (no overflow with `BigDecimal`, but there are business limits), and the statement
returned to callers being unmodifiable.

**Security and privacy.** `InsufficientFundsException` in this kata includes the balance in its message. That is handy in an interview,
but in production messages reach logs and APMs, so prefer a code plus the account id and keep the balance out (module08 `LogSanitizer`).

**In-memory vs production.**
| Concern | Kata | Production |
|---|---|---|
| Atomicity | `synchronized (lock)` | DB transaction; conditional `UPDATE ... SET balance = balance - ? WHERE id = ? AND balance >= ?` |
| Audit trail | in-memory `List<Transaction>` | append-only ledger table, immutable, retained per regulation (often 5-10 years) |
| Multiple instances | not handled | DB is the single source of truth; or one owner per account partition |

**Common mistakes live.** `double` for money; `equals` on `BigDecimal`; returning the internal list; locking on `this`;
forgetting to record the transaction inside the same critical section as the balance change.
