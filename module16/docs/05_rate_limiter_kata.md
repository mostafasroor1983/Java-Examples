# 05 - Kata: Rate limiter (`com.revolut.interview.ratelimiter`)

Prompt: "Limit each client to N requests per window." Three implementations behind one interface:

| Algorithm | Memory / key | Exactness | Burst behaviour | Class |
|---|---|---|---|---|
| Fixed window | O(1) | approximate, up to 2x at the boundary | allows 2N in a short span | `FixedWindowRateLimiter` |
| Sliding log | O(N) | exact | none | `SlidingWindowRateLimiter` |
| Token bucket | O(1) | exact on average | burst up to capacity, then rate | `TokenBucketRateLimiter` |

Implementation notes
- Inject the clock (`LongSupplier`) so tests can set time precisely.
- Per-key state in a `ConcurrentHashMap`; synchronise on the per-key object, never globally.
- Fixed window keys by `(key, windowIndex)` so old windows are abandoned; `purge()` cleans them up.
- All three are proven to admit exactly 100 of 800 concurrent requests.

What to say about distributed rate limiting
- Store counters in Redis with `INCR` + `EXPIRE` (fixed window) or a Lua script for token buckets.
- Accept approximate limits across nodes or route by key (consistent hashing) for exactness.
- Return `429` with `Retry-After`; shed load before the expensive path.

## Deeper dive
**Edge cases to test.** Exactly at the window boundary; clock going backwards (use `System.nanoTime`-style monotonic clocks);
a burst after long idleness (token bucket refill must be capped at capacity); thousands of distinct keys (memory: purge idle keys).

**Fintech-specific choices.**
| Use | Algorithm | Why |
|---|---|---|
| Public API per client key | token bucket | allows short bursts, smooth long-term rate |
| Login / OTP attempts | sliding log (exact) + lockout | security: no boundary burst |
| Outbound calls to a PSP with a contract limit | token bucket on *our* side + bulkhead | never exceed the provider's limit (module12 `AsyncBulkhead`) |
| Fraud velocity rules ("max 5 card payments in 10 min") | sliding window per card, persisted | must survive restarts and be consistent across nodes |

**Failure modes.** A per-node in-memory limiter with N nodes allows N x limit. A shared store (Redis) adds a network hop.
If Redis is down, decide explicitly whether to fail open (availability) or fail closed (protection); for login and OTP, fail closed.
