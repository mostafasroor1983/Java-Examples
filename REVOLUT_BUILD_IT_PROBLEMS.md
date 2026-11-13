# Revolut "Build It" problems - prompts, requirements and where the reference lives

Use these as timed exercises (45-60 minutes each). Read the prompt, write the invariants, implement with tests, then compare with the reference in module17 (`com.revolut.buildit.*`) and its approach note.

## 1. Load balancer
Prompt: "Build a load balancer that registers up to 10 backend addresses, rejects duplicates, and returns one address per call using a pluggable strategy. Unhealthy backends must be skipped."
Must have: capacity + duplicate rules, round robin and random strategies, health flags, thread-safe `next()`.
Reference: `loadbalancer`, `docs/approach_01_loadbalancer.md`.

## 2. Shopping cart
Prompt: "Build a cart supporting add/update/remove lines, subtotal, and discounts: percentage coupon, buy-2-get-1, free shipping over a threshold. Totals must never be negative."
Must have: merged quantities, pure discount rules over a snapshot, discount cap, exact rounding.
Reference: `shoppingcart`, `docs/approach_02_shoppingcart.md`.

## 3. Money transfer
Prompt: "Build a money transfer system: open accounts, transfer with idempotency, daily outbound limit, per-account history. Concurrent opposite transfers must be safe."
Must have: typed outcomes, ordered locks, `putIfAbsent` idempotency, limit reset by day, conserved total under concurrency.
Reference: `moneytransfer`, `docs/approach_03_moneytransfer.md`.

## 4. Subscription service
Prompt: "Build subscriptions with Standard/Plus/Premium/Metal plans, monthly billing, immediate plan changes with daily proration, cancel at period end, and a billing run."
Must have: proration formula with rounding policy, status transitions, idempotent billing run, injected date.
Reference: `subscription`, `docs/approach_04_subscription.md`.

## 5. Currency exchange
Prompt: "Build a multi-currency wallet with rate publication, quotes with a spread, and atomic exchange between pockets. Quotes on outdated rates must be rejected."
Must have: immutable versioned rate snapshot, cross rates, rounding down, stale-quote rejection, per-customer pocket atomicity.
Reference: `currencyexchange`, `docs/approach_05_currencyexchange.md`.

## 6. URL shortener
Prompt: "Build a URL shortener: short codes, custom aliases, expiry, click counts. The same URL must map to the same code."
Must have: base62 of a sequence, two indexes with race handling, lazy expiry, URL validation (http/https only).
Reference: `urlshortener`, `docs/approach_06_urlshortener.md`.

## 7. Tiered API rate limiter
Prompt: "Build an API rate limiter with per-tier limits (free/pro/enterprise), a global ceiling, and a Retry-After hint. Memory per client must be O(1)."
Must have: sliding window counter with weighted previous window, per-client and global state, exact admission under concurrency.
Reference: `ratelimiter`, `docs/approach_07_ratelimiter.md`.

## 8. Double-entry ledger
Prompt: "Build a ledger where every entry has postings that balance per currency, the journal is append-only, balances are derived, entries are idempotent by id, and balances can be reconciled against the journal."
Must have: compact-constructor validation, id claiming, RW lock, `reconcile()`, `systemTotal == 0`.
Reference: `ledger`, `docs/approach_08_ledger.md`.

## Also practise (module16 and module15)
- Bank account, transfer, payment service and rate limiter katas: `module16` (`com.revolut.interview.*`) with guides `01`-`08`.
- Fifteen smaller problems (counter, balance, transfer, state machine, idempotency, dedup, limiter, LRU, producer-consumer, aggregator, batch, retry, scheduler, sliding window, history): `module15`.

## Scoring yourself
| Criterion | Check |
|---|---|
| Invariants | stated before coding, each one has a test |
| Correctness | happy path + every typed outcome |
| Concurrency | exact totals under N threads, no sleeps, no deadlock |
| Money | BigDecimal, explicit rounding policy |
| Design | small immutable values, one clear aggregate, injected clock |
| Communication | trade-offs named (what you would do with a DB, at scale, with retries) |
