# Approach 03 - Money Transfer (`com.revolut.buildit.moneytransfer`)

Requirements: open accounts, transfer with typed outcomes, idempotency by transfer id, per-account history, daily outbound limit, money conserved, no deadlock.

Design
- Per-account `ReentrantLock`; locks acquired in id order.
- `byId.putIfAbsent(transferId, placeholder)` claims the id before executing; replays return `DUPLICATE`.
- Daily limit tracked per account with `rollDay` on the injected clock (UTC date).
- Failed attempts are recorded in the sender's history; completed ones in both.

Trade-offs: DB implementation (`SELECT ... FOR UPDATE` in id order, or optimistic `version`), limits in a separate service, time zones for "daily",
compensation when the credit leg fails (not possible in-memory, essential across services), outbox events.

Tests: every outcome, idempotent replay, limit reset at midnight, 6 opposing threads conserve 3000 within 30 s.

## Deeper dive
**Daily limit race.** "Read today's total, then write" in two steps lets two concurrent transfers both pass. The limit is checked and
updated **under the account lock**, in the same critical section as the debit. In a DB, the same rule becomes a conditional update or row lock.
**Time zones.** "Daily" must be defined: UTC, the customer's zone, or the regulator's? Inject the clock and the zone; test the midnight boundary.
**Failure atomicity.** In memory the credit cannot fail after validation. Across services it can, so you need a saga (reserve, credit,
confirm or compensate) and reconciliation for sagas stuck mid-way. See module16 `09_production_hardening.md`.

**Bugs fixed in review (regression tests in `MoneyTransferRegressionTest`).**
- *Daily-limit bypass at midnight:* the timestamp is read before the account lock is taken, so a thread stamped 23:59:59 could run
  after one stamped 00:00:01 and roll the day *back*, wiping today's spent amount. `rollDay` now only moves forward.
- *Burned transfer id:* an unexpected exception left the idempotency placeholder in place forever. It is now released so the client can retry.
