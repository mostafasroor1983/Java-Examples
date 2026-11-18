# Approach 08 - Double-Entry Ledger (`com.revolut.buildit.ledger`)

Requirements: journal entries with postings that balance per currency, append-only, derived balances, idempotent by entry id, reconciliation.

Design
- `Posting(account, currency, amount)`; `JournalEntry` validates in its compact constructor: >= 2 postings, per-currency sum zero (`UnbalancedEntryException`).
- Validation happens before the lock; `ids.add(id)` claims the id (CHM key set); `ReentrantReadWriteLock` guards journal + balances.
- Balances are a projection updated under the write lock; `reconcile()` rebuilds from the journal and reports mismatches; `systemTotal(currency)` must be zero.
- `transfer()` is sugar for a two-posting entry.

Trade-offs: persistence as append-only table + balance snapshots, partitioning by account with cross-shard entries via saga, immutability of history (reversals are new entries), audit/retention, FX legs balanced per currency with a P&L account absorbing rounding.

Tests: balances and zero system total, idempotent id, unbalanced/single/zero postings rejected with nothing appended, multi-currency entry, 8 threads x 500 transfers reconcile cleanly.

## Deeper dive: reversals (implemented)
`Ledger.reverse(reversalId, originalId, reason)` appends a new entry with every posting negated. History is never edited.
- Each original can be reversed **once**, enforced by an atomic claim (`ConcurrentHashMap.newKeySet().add`). 32 concurrent reversal
  requests post exactly one (`LedgerReversalTest`).
- A reversal cannot itself be reversed (re-post the original instead), and a reason is mandatory, because auditors ask "why".
- If the reversal id clashes with an existing entry, the claim is released so a correct retry can succeed.
- Property test: 2,000 random transfers in two currencies with about 10% random reversals always reconcile, with a zero system total per currency.

**Regulatory notes.** Posted entries are immutable; corrections are new entries with who, why and when; retention follows local rules
(often 5-10 years); access to post manual adjustments should require maker-checker approval (module01 `ddd.PaymentInstruction`).
