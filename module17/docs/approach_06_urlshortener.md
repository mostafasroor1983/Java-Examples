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

## Deeper dive
**Security.** Only `http(s)` absolute URLs: blocks `javascript:` and `data:` schemes (XSS through redirects). In fintech contexts
(payment links, statements) add domain allow-lists, or at least a malware/phishing check, since short links are a phishing vector.
**Predictability.** Sequential base62 codes are enumerable: anyone can walk the code space. For links to private documents use random,
unguessable tokens (>= 128 bits) and authentication, never a sequence.
**Production.** Distributed id generation (per-node ranges or Snowflake), 301 vs 302 (caching vs analytics), and GDPR deletion requests.
