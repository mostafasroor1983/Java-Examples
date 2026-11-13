# Module 17 - Revolut "Build It" (study guide)

Package root: `com.revolut.buildit`. See `docs/README.md` for the index and one `approach_NN_*.md` per system.

## The shape of every solution
1. Requirements -> invariants (money conserved, no overdraft, idempotent id, balanced entry).
2. Immutable records for values; one mutable aggregate with a clear lock or atomic swap.
3. Typed outcomes (`enum`) for business results; exceptions for invalid input/config.
4. Injected clock (`Supplier<Instant>`, `Supplier<LocalDate>`, `LongSupplier`).
5. Tests: happy path, every outcome, concurrency with exact totals.

## BAD vs GOOD across the eight
```java
// BAD: random short codes with retry-on-collision        GOOD: base62(sequence) - collision-free by construction
// BAD: rate table mutated in place (torn reads)          GOOD: immutable RateSnapshot in AtomicReference, versioned quotes
// BAD: balances as the source of truth                   GOOD: append-only journal, balances as a reconcilable projection
// BAD: discounts hard-coded in totals()                  GOOD: Discount functions over an immutable Snapshot
// BAD: daily limit read then written (race)              GOOD: checked and updated under the account lock
```

## Key Takeaways
- Idempotency keys appear in 4 of 8 systems; make `putIfAbsent`/unique-constraint claiming a reflex.
- Versioned immutable snapshots solve "atomic config/rate update" without locks.
- A ledger's invariant is per-currency zero sum per entry; everything else is projection.
- Sliding window counter is the pragmatic middle between fixed windows and exact logs.
- Proration and rounding need a stated policy (HALF_EVEN for fairness, DOWN when the house must not lose).

## Cheat Sheet
| System | Core structure | Concurrency tool | Test that matters |
|---|---|---|---|
| Load balancer | COW list + health map | atomic counter | even split under 8 threads |
| Cart | LinkedHashMap lines | synchronized cart | exact pennies with stacked discounts |
| Transfer | per-account lock, id order | putIfAbsent idempotency | opposing transfers conserve total |
| Subscription | records + compute() | CHM compute | proration pennies, cancellation at period end |
| FX | AtomicReference snapshot | per-wallet monitor | stale quote rejected, pockets never overdrawn |
| Shortener | two CHMs + sequence | putIfAbsent race fix | one code for 16 concurrent shortens |
| Rate limiter | sliding counter windows | per-client monitor + global | weighted window, global ceiling exact |
| Ledger | journal + projection | RW lock + id set | reconcile() empty after 4000 concurrent entries |

## 10 Interview Q&A
1. **Why base62 of a sequence rather than a hash?** No collisions, short codes, O(1); hashes need collision handling and are longer.
2. **How do you make rate updates atomic for readers?** Build a new immutable snapshot and swap the reference; quotes carry the version.
3. **Why round FX results down?** The institution must not lose fractions of a cent to rounding; state the policy explicitly.
4. **Why is the journal append-only?** Auditability and rebuildability; corrections are new reversing entries.
5. **What does `reconcile()` protect against?** Bugs in the projection path; in production it compares DB balances to journal sums nightly.
6. **Fixed vs sliding counter vs token bucket for an API?** Counter gives smooth limits with O(1) memory; bucket allows bursts; fixed is cheap but gamed at boundaries.
7. **How do you prorate a plan change?** Price difference times remaining days over period days; credit when negative; apply at the next invoice or immediately.
8. **How does the cart keep totals non-negative?** Cap total discount at subtotal + shipping before subtracting.
9. **How would you scale the transfer system?** Partition by account id; same-shard transfers lock locally; cross-shard via saga with reserve/confirm.
10. **How do you test these without flakiness?** Inject clocks, start threads with a latch, await with a timeout, assert exact totals.

## Exercises for the reader
1. Add weighted round robin to the load balancer and prove the ratio under 8 threads.
2. Give the ledger a `reverse(entryId)` that posts the negated postings and keeps `reconcile()` clean.
3. Add quote expiry by time to `CurrencyExchange` in addition to version and test both rejections.
