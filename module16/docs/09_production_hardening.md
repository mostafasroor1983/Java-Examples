# 09 - From kata to production: what changes

A kata proves you can write correct code in one JVM. Interviewers for senior roles then ask: *"What would you change to run
this at Revolut scale?"* This guide is the structured answer.

## 1. Source of truth moves to the database
| Kata mechanism | Production replacement | Guarantee it gives |
|---|---|---|
| `synchronized` / `ReentrantLock` around balance | conditional `UPDATE` or `SELECT ... FOR UPDATE` in a transaction | atomicity across all service instances |
| `ConcurrentHashMap` idempotency map | table with `UNIQUE(idempotency_key)` + TTL cleanup | exactly-once effect per key, survives restarts |
| Lock ordering by account id | lock rows in id order inside the transaction | no DB deadlocks between opposite transfers |
| In-memory transaction list | append-only `ledger_entry` table (double entry) | audit trail, reconciliation, regulatory retention |

> In-memory locks never protect you across processes. Two pods running the kata code would both approve a withdrawal.

## 2. Events and integration
- Write domain events to an **outbox** table in the same transaction (module15 P16), and relay them to the broker.
- Consumers are **idempotent** (dedup table in their own transaction) and tolerate **reordering** (version or sequence checks).
- Per-account ordering comes from partitioning by account id (module15 P17 is the in-process equivalent).

## 3. Resilience
- Every outbound call has a timeout, bulkhead, circuit breaker and bounded retries with jitter (modules 08, 12).
- Timeouts on money movement are **unknown outcomes**: query status by idempotency key, or reconcile; never assume failure.
- Graceful degradation: serve cached FX rates with an explicit staleness limit; queue non-critical notifications.

## 4. Observability
- Metrics: request rate, error rate, p50/p99 latency, queue depths, pool saturation, retry and rejection counts, outbox lag.
- Logs: structured, correlation id on every line, **no PANs/IBANs/secrets** (module08 `LogSanitizer`), no balances in messages.
- Tracing across async hops (module09 context propagation, module13 `ScopedValue`).
- Alerts on business signals too: reconciliation breaks, unknown-outcome backlog, dead letters.

## 5. Security and regulatory concerns
- **Separation of duties** for high-value or manual operations (module01 maker-checker).
- **Least privilege** for services and operators; audit every privileged action with who, what, when and why.
- **Data minimisation and retention:** keep what regulation requires (ledger: years), delete or anonymise the rest (GDPR).
- **Integrity:** append-only ledger, no `UPDATE` of posted entries; corrections are new reversing entries.

## 6. Scalability and performance
- Hot accounts (large merchants) serialise on one row. Use sub-accounts/sharded balances, or batch credits.
- Virtual threads (Java 21) for I/O-bound handlers, with semaphores sized to DB pools (module13).
- Measure with load tests at the target percentile, and profile with JFR before tuning (module14).

## Interview answer template
1. "In this kata the invariant is guarded by X in one JVM."
2. "In production the source of truth is the DB: I'd enforce it with Y (constraint / conditional update / row lock)."
3. "Events go through an outbox; consumers are idempotent."
4. "Calls out are bounded by timeouts, bulkheads and breakers; timeouts become unknown outcomes and are reconciled."
5. "I'd monitor A, B, C and alert on reconciliation breaks."
