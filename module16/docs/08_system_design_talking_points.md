# 08 - System design talking points (payments at Revolut scale)

Ledger
- Double-entry: every movement is a debit and a credit; the sum of a transaction's legs is zero.
- Append-only journal; balances are projections (cached, rebuildable). Reconcile nightly.
- Idempotency keys on every write API; unique constraint in the DB is the final guard.

Exactly-once and the outbox
- Write the business row and an outbox event in one DB transaction; a relay publishes to Kafka; consumers deduplicate by event id (module15 P06).
- Retries everywhere assume at-least-once; design handlers to be idempotent.

Consistency choices
- Strong consistency per account (single writer / partition by account id); eventual consistency across accounts via saga (reserve -> confirm -> release).
- Optimistic locking (`version` column) for low contention; pessimistic `SELECT ... FOR UPDATE` for hot accounts.

Scaling
- Shard by customer/account id; keep both legs of a transfer on the same shard when possible, otherwise saga.
- Hot keys (merchant payout accounts): batch credits, or sub-accounts aggregated asynchronously.
- Rate limiting at the edge (module16 limiters), bulkheads per downstream (module11), circuit breakers (module08).

Resilience
- Timeouts on every call, bounded retries with jitter, fallbacks that route to manual review rather than silently approving.
- Multi-region: active-active for reads, single-writer per account region with failover; reconcile on recovery.

Observability
- Trace id propagated (module13 ScopedValue), metrics per outcome (approved/declined/failed), alerts on reconciliation drift and outbox lag.

Questions to ask back
- Which consistency is required between the customer debit and the merchant credit?
- Expected TPS and p99 latency budget? Peak multipliers (Black Friday)?
- Regulatory constraints on data residency and audit retention?
