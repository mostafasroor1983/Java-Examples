# Approach 07 - Tiered API Rate Limiter (`com.revolut.buildit.ratelimiter`)

Requirements: per-client limit depending on tier, global ceiling, `Retry-After` hint, O(1) memory per client.

Design
- Sliding window **counter**: current window count + previous window count weighted by the overlap fraction. Approximates the exact sliding log with two integers.
- Per-client `Window` objects in a `ConcurrentHashMap`, synchronised individually; the global `Window` is checked and incremented inside the client lock (lock order client -> global) so the ceiling is exact under concurrency.
- `Decision(allowed, remaining, retryAfterMillis)` maps directly to HTTP 429 headers.
- Windows that were skipped entirely carry nothing over (`roll`).

Trade-offs: token bucket for burst-friendly APIs, exact log for strict SLAs, distributed counters in Redis (`INCR`/`EXPIRE` or Lua), per-endpoint costs (weights), tier changes mid-window.

Tests: remaining/retry-after, weighted previous window (10 then 5 allowed at half window), skipped windows reset, global ceiling across 10 clients admits exactly 50.

## Deeper dive
**Lock order client -> global** makes the global ceiling exact: a request takes its client's window lock, then the global window's lock.
Every request uses the same order, so there is no deadlock. The cost is that the global lock is a contention point; at scale, use
striped or approximate global counters.
**Edge cases.** Tier upgrade mid-window (new limit applies immediately); client clock skew is irrelevant (server clock only); the
`Retry-After` value must never be negative or zero when denying.
**Production.** Limits enforced at the edge (API gateway) *and* in the service for expensive operations; shared state in Redis with
Lua for atomicity; explicitly fail open or fail closed when Redis is unavailable (fail closed for login, OTP and payments initiation).
