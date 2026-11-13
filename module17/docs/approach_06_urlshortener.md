# Approach 06 - URL Shortener (`com.revolut.buildit.urlshortener`)

Requirements: shorten (idempotent per URL), resolve with click counting, custom aliases, expiry, input validation.

Design
- Code = base62 of an `AtomicLong` sequence: no collisions, no random retries, codes grow slowly (start at 100 000 for >= 3 chars).
- Two maps: `byCode` and `codeByUrl`; `putIfAbsent` race handling so 16 concurrent shortens of one URL yield one code.
- Expiry checked lazily on resolve; entries removed when found expired.
- `LongAdder` for clicks (write-heavy).
- Validation: absolute `http(s)` with a host only (blocks `javascript:` and relative URLs), length cap 2048, alias regex.

Trade-offs: distributed id generation (Snowflake / per-node ranges), 301 vs 302 and click analytics, hot-key caching, abuse/rate limiting, GDPR deletion.

Tests: base62 round trips including `Long.MAX_VALUE`, idempotent shorten, counting, expiry, aliases, validation, concurrency.
