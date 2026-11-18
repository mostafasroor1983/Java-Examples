# Approach 04 - Subscription Service (`com.revolut.buildit.subscription`)

Requirements: plans with monthly prices, subscribe, upgrade/downgrade with daily proration, cancel at period end, billing run.

Design
- `Plan` enum carries the price; `Subscription` record with `periodStart`/`nextBillingDate`; `Status` ACTIVE / PENDING_CANCELLATION / CANCELLED.
- `changePlan` charges `(new - old) * remainingDays / periodDays` rounded `HALF_EVEN`; negative = credit.
- `runBilling()` renews everything due on or before today and finalises pending cancellations; idempotent for a given day because `nextBillingDate` moves forward.
- `LocalDate` clock injected; charges recorded in an audit list.

Trade-offs: anchor billing day vs calendar month, grace periods and dunning on failed payments, annual plans, trials, tax, timezone of "today".

Tests: initial charge, proration up and down with exact pennies, no-op change, renewal moves dates, cancellation finalises without a charge, free plan records nothing.

## Deeper dive
**Proration correctness.** Use whole days with an explicit period length; round `HALF_EVEN` once; a downgrade produces a *credit*, not a
negative charge on the card. Month-end anchors (31 Jan -> 28/29 Feb -> 31 Mar) need a stated rule.
**Idempotent billing run.** Running the job twice on the same day must not double charge: `nextBillingDate` advancing is the guard here.
In production use a unique `(subscription_id, period_start)` constraint on invoices, because jobs *will* run twice (deploys, retries, two pods).
**Failure scenarios.** Card declined at renewal leads to dunning (retry schedule, grace period, then suspension), not immediate cancellation.
Notify before charging where regulation requires it (e.g. trial-to-paid conversions).

**Bugs fixed in review (regression tests in `SubscriptionBillingRegressionTest`).**
- *Cancelled subscriptions were resurrected:* after a cancellation was finalised, the next billing run charged a renewal and set the
  status back to ACTIVE. Cancelled subscriptions are now skipped.
- *Double charge on overlapping billing runs:* "is it due?" was checked outside `compute`. Every condition is now re-checked inside the
  atomic `computeIfPresent`, and 4 concurrent runs over 200 subscriptions charge each one exactly once.
Lesson: a check outside an atomic operation is a check-then-act race, even when the update itself is atomic.
