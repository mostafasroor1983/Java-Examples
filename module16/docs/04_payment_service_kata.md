# 04 - Kata: Payment processing (`com.revolut.interview.payment`)

Prompt: "Design a service that creates, authorises, captures and refunds card payments with fraud checks."

Model
- `Payment` record with `PaymentStatus` and `with(next, reason, at)` for transitions.
- `FraudCheck` functional interface; checks are composed in a list (OCP).
- `PaymentService`: `create` is idempotent (`computeIfAbsent`), transitions are atomic per payment (`computeIfPresent`), a `Supplier<Instant>` clock makes timestamps deterministic.

State machine
```
PENDING --authorise--> AUTHORISED --capture--> CAPTURED --refund--> REFUNDED
   |                        |--refund--> REFUNDED (release hold)
   |--fraud/funds--> DECLINED
   |--unknown account--> FAILED
```
Money movement: authorise withdraws from the customer (hold), capture deposits to the merchant, refund reverses.

Tests (`PaymentServiceTest`)
- full flow with balances after each step and `updatedAt` from the injected clock
- fraud decline reasons, insufficient funds, unknown customer -> FAILED, declines never move money
- illegal transitions throw `IllegalStateException`; refund from AUTHORISED releases the hold

Follow-ups
- Idempotent authorise with provider retries (store provider reference).
- Partial captures and multiple refunds (track captured/refunded amounts).
- Async: fraud scoring with timeout -> manual review (module12 `PaymentWorkflow`).
- Persistence: payments table + state transition audit table; optimistic locking on `version`.
