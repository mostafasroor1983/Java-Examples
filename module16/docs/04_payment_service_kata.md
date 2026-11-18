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

## Deeper dive
**New concurrency tests** (`PaymentServiceConcurrencyTest`):
- 16 threads capture the same payment simultaneously: exactly one succeeds and the merchant is credited once. The same holds for refunds.
- 50 parallel authorisations of 7.00 against a 100.00 balance: exactly 14 are authorised and the balance ends at 2.00 (no overdraft).

Why it works: `computeIfPresent` serialises all transitions *per payment id* (CHM bin lock), and `Account` serialises balance
changes. Note the nesting: a CHM bin lock is held while taking an account lock. That is safe here because nothing takes them in the
reverse order, but it is the kind of hidden lock ordering you should call out.

**Known limitations to discuss.**
- `create` with an existing id but a different amount silently returns the original payment. Safer: compare payloads and
  reject a conflict (module15 P05).
- Refund from CAPTURED withdraws from the merchant. If the merchant was already paid out, it throws and the refund is blocked.
  Real systems refund from a platform float and recover from the merchant later (negative balance or chargeback flow).
- Provider calls (card scheme authorisation) would happen *outside* the map lock: never do network I/O inside `compute`.
  A timeout there is an ambiguous outcome (module08 `AmbiguousOutcomeHandler`).
- Real lifecycles include partial capture, multiple partial refunds, reversals and expiry of uncaptured holds (module07 `lifecycle`).
