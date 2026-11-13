# Module 02 - equals/hashCode, Value Objects and Immutability

Package root: `module02`. Domain: accounts, payment keys, FX quotes, config.

## The contract everyone gets asked
`equals` must be reflexive, symmetric, transitive, consistent, and `x.equals(null)` is false.
If `a.equals(b)` then `a.hashCode() == b.hashCode()`. Breaking the second rule is the single most
common production bug with `HashMap`, `HashSet`, `ConcurrentHashMap`, and every cache built on them.

## BAD vs GOOD

### Missing hashCode
```java
// BAD - module02.equalshashcode.BrokenAccount
@Override public boolean equals(Object o) { ... iban.equals(...) }
// hashCode() not overridden -> identity hash -> HashSet.contains() returns false
```
```java
// GOOD - module02.equalshashcode.Account
@Override public boolean equals(Object o) { ... iban.equals(...) }
@Override public int hashCode() { return iban.hashCode(); }
```

### Asymmetric equals (`AsymmetricCard`)
A subclass comparing an extra field while the superclass does not breaks symmetry:
`plain.equals(scheme) == true` but `scheme.equals(plain) == false`. Fix: make value classes `final`,
or use `getClass()` comparison, or (best) use records.

### Mutable keys (`module02.mutablekey`)
```java
map.put(key, TEN);
key.setOrderId("o2");   // hash drifts
map.get(key);           // null  - entry is orphaned in the old bucket
map.size();             // 1     - memory leak in long-running services
```
GOOD: `record PaymentKey(String merchantId, String orderId)`.

### BigDecimal scale trap (`module02.valueobjects.Money`)
`new BigDecimal("1.0").equals(new BigDecimal("1.00"))` is **false**, `compareTo` says 0.
`Money` normalises scale in the constructor with `RoundingMode.UNNECESSARY`, so sub-penny input is rejected.

### Records are shallowly immutable (`module02.records`)
`LeakyPayment(String id, List<String> tags)` keeps the caller's list. `Payment` does `tags = List.copyOf(tags)`
in the compact constructor.

### Immutability (`module02.immutability`)
BAD `MutableFxQuote`: setters, leaks `Date` and `List`. GOOD `FxQuote`: `final` fields, `Instant`,
`List.copyOf`, `withRate()` returns a new instance.

## Exercises
1. **PaymentLedger (HashMap bug)** - `PaymentRef` overrides `equals` only. `find()` returns empty and duplicates
   are stored twice. `PaymentLedgerFixed` uses a record key and `putIfAbsent` for idempotency.
2. **ConfigHolder** - aliases the caller's map, exposes it via getter; `Collections.unmodifiableMap` is a *view*.
   `ImmutableConfig` uses `Map.copyOf` and a `with()` method.
3. **Reader**: make `Email` reject disposable domains without breaking equality semantics.

## Key Takeaways
- Override both or neither. Use the same fields in both.
- Records solve 90 % of value-object boilerplate; still validate and copy in the compact constructor.
- Never mutate an object after using it as a key. Prefer immutable keys.
- `BigDecimal.equals` is scale-sensitive; normalise or compare with `compareTo`.
- `Collections.unmodifiableX` is a view; `List.copyOf` / `Map.copyOf` are copies.
- Immutable objects are inherently thread-safe and can be freely cached and shared.

## Cheat Sheet
| Question | Answer |
|---|---|
| Fields in hashCode but not equals? | Allowed but useless |
| Fields in equals but not hashCode? | **Bug** |
| `instanceof` or `getClass()`? | `instanceof` if class is final or equality is "by contract"; `getClass()` to forbid subclass equality |
| Records and inheritance? | Records cannot extend classes; they are implicitly final |
| `Objects.hash` cost | Allocates a varargs array; fine unless on a hot path |
| Cache hashCode? | Only for immutable objects (String does it) |

## 10 Interview Q&A
1. **What happens if hashCode is inconsistent with equals?** Equal objects can land in different buckets; `get` fails, duplicates appear in sets.
2. **Can two unequal objects have the same hashCode?** Yes, that is a collision; HashMap resolves it via `equals` within the bucket.
3. **Why is `Objects.hash(a, b)` sometimes avoided?** Varargs allocation plus boxing on hot paths; hand-roll `31 * h + ...` if measured.
4. **Why do records make good map keys?** Final, structural equals/hashCode generated from all components, no setters.
5. **What is wrong with `Date` in a value object?** It is mutable; return a copy or use `java.time` types.
6. **How does `BigDecimal` break value semantics?** Scale is part of equality. `1.0 != 1.00`. Normalise scale or use `compareTo`.
7. **Is `Collections.unmodifiableList(list)` immutable?** No, it is a read-only view over a possibly mutating list.
8. **How do you make a class immutable with a mutable dependency?** Copy on the way in, copy or wrap on the way out, never expose the reference.
9. **Why should value objects be `final`?** To keep `equals` symmetric; subclasses cannot redefine equality.
10. **Thread-safety of immutable objects?** Safe publication is guaranteed by `final` fields (JMM final-field semantics) even without synchronisation.

## Exercises for the reader
1. Add an `Iban` value object with checksum validation (mod-97) and prove equality after normalising whitespace/case.
2. Write a `HashMap` stress test: insert 10 000 `PaymentRef`s then look them all up; count misses.
3. Implement `ConfigHolder.v2` using a builder that validates that `fx.spread.bps` is an integer in [0, 10000].
