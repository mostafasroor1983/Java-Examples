# Approach 05 - Currency Exchange (`com.revolut.buildit.currencyexchange`)

Requirements: publish rates atomically, quote with spread, multi-currency pockets, execute a quote atomically, reject stale quotes.

Design
- `RateSnapshot` (immutable map + version) swapped in an `AtomicReference`: readers never see a torn table.
- Cross rates via USD; spread in basis points applied to the mid; result rounded **down** so rounding never favours the customer.
- `Quote` carries the rate version; `exchange` rejects if rates moved (`STALE_QUOTE`) - the in-memory analogue of a quote TTL.
- Two-pocket atomicity per customer by synchronising on the customer's wallet map.

Trade-offs: quote expiry by time as well as version, bid/ask instead of a symmetric spread, weekend markups, hedging and position limits,
rounding policy per currency (JPY has 0 decimals).

Tests: exact rate and rounding, cross rates, unknown/same/zero rejected, stale quote after republish, 8 threads x 50 exchanges drain exactly 100 GBP.

## Deeper dive
**Why a versioned snapshot.** A quote must be executable at the price shown. The quote carries the rate version, so execution against a
newer table is rejected (`STALE_QUOTE`) instead of silently filling at a different price. This is the same pattern as module10 `QuoteBook`.
**Rounding policy.** Rounding *down* on the customer's receive side means the institution never pays out fractions it did not price.
That must be disclosed and applied consistently. Per-currency scale matters (JPY 0, BHD 3).
**Production.** Rates come from liquidity providers with timestamps; reject quotes older than N seconds as well as stale versions; apply
weekend/illiquid-pair markups; track exposure and position limits per currency; log every executed quote for best-execution audits.

**Security bug fixed in review (`CurrencyExchangeSecurityTest`).** `Quote` is a public record, so a caller could construct a quote with
the current rate version and an inflated `result`, and `exchange` would credit it. `exchange` now re-derives the quote from the same
snapshot and returns `INVALID_QUOTE` on any mismatch. Rule: never trust prices or amounts that round-trip through the client.
Production alternative: store issued quotes server-side (quote id -> terms, expiry) and accept only the id.
