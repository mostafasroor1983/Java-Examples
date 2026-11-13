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
