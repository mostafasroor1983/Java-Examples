# Approach 04 - Subscription Service (`com.revolut.buildit.subscription`)

Requirements: plans with monthly prices, subscribe, upgrade/downgrade with daily proration, cancel at period end, billing run.

Design
- `Plan` enum carries the price; `Subscription` record with `periodStart`/`nextBillingDate`; `Status` ACTIVE / PENDING_CANCELLATION / CANCELLED.
- `changePlan` charges `(new - old) * remainingDays / periodDays` rounded `HALF_EVEN`; negative = credit.
- `runBilling()` renews everything due on or before today and finalises pending cancellations; idempotent for a given day because `nextBillingDate` moves forward.
- `LocalDate` clock injected; charges recorded in an audit list.

Trade-offs: anchor billing day vs calendar month, grace periods and dunning on failed payments, annual plans, trials, tax, timezone of "today".

Tests: initial charge, proration up and down with exact pennies, no-op change, renewal moves dates, cancellation finalises without a charge, free plan records nothing.
