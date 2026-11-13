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
