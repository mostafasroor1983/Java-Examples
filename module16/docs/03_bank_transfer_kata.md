# 03 - Kata: Money transfer (`com.revolut.interview.bankTransfer`)

Prompt: "Transfer money between two accounts. Concurrent transfers in both directions must be safe."

Invariants
- Atomic: debit and credit both happen or neither.
- No negative balance, money conserved across the system.
- No deadlock for A->B concurrent with B->A.
- Idempotent per request id; same account and unknown accounts rejected; currency must match.

Design (`TransferService`)
- Per-account `ReentrantLock` in a `ConcurrentHashMap`, acquired in **id order** (lower id first).
- Idempotency: `processed.putIfAbsent(requestId, placeholder)` claims the id; duplicates get `DUPLICATE`.
- Validation before locking; `InsufficientFundsException` mapped to a typed `Status`.
- Result is a record (`TransferResult`) rather than exceptions for expected outcomes.

Tests (`TransferServiceTest`)
- completed + balances, duplicate request does not move money twice
- insufficient funds / same account / unknown / currency / zero amount leave balances untouched
- 6 threads x 1000 opposing transfers between A, B, C finish within 30 s and total stays 3000

Talking points
- Alternatives to lock ordering: `tryLock` with back-off (module11), single-writer actor per account, optimistic versioning in a DB (`UPDATE ... WHERE version = ?`).
- Distributed version: saga with reserve/confirm/cancel, or a double-entry ledger with an outbox for events.
- Observability: emit `transfer.completed/declined` metrics, trace ids in `TransferRequest`.
