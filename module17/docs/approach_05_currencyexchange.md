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
