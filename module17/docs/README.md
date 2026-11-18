# Module 17 - Revolut "Build It" problems

Eight small systems in `com.revolut.buildit.*`, each with an approach note in this folder and AssertJ tests proving contracts, error paths and concurrency.

| # | System | Package | Approach |
|---|---|---|---|
| 1 | Load balancer | `loadbalancer` | [approach_01_loadbalancer.md](approach_01_loadbalancer.md) |
| 2 | Shopping cart | `shoppingcart` | [approach_02_shoppingcart.md](approach_02_shoppingcart.md) |
| 3 | Money transfer | `moneytransfer` | [approach_03_moneytransfer.md](approach_03_moneytransfer.md) |
| 4 | Subscription service | `subscription` | [approach_04_subscription.md](approach_04_subscription.md) |
| 5 | Currency exchange | `currencyexchange` | [approach_05_currencyexchange.md](approach_05_currencyexchange.md) |
| 6 | URL shortener | `urlshortener` | [approach_06_urlshortener.md](approach_06_urlshortener.md) |
| 7 | Tiered rate limiter | `ratelimiter` | [approach_07_ratelimiter.md](approach_07_ratelimiter.md) |
| 8 | Double-entry ledger | `ledger` | [approach_08_ledger.md](approach_08_ledger.md) |

Each approach note ends with a **Deeper dive** (edge cases, failure scenarios, production notes). The ledger now includes immutable reversals (`Ledger.reverse`).

How to use: read the approach, close it, implement from the requirements in 45 minutes, then diff against the reference. Run `mvn -pl module17 test` from the repo root.
