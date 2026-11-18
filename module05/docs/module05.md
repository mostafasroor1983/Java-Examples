# Module 05 - Generics

Package root: `module05`. Domain: typed IDs, repositories, transfer results.

## Overview
Generics let the compiler reject whole classes of financial bugs before code ships: passing an account id where a
wallet id is expected, adding euros to dollars, applying an FX rate in the wrong direction, or forgetting to handle a
failed transfer. This module covers generic classes and methods, bounds, wildcards (PECS) and erasure, then applies
them to **phantom-typed currencies** (`currency.Amount<C>`, `currency.FxRate<F, T>`) and a **type-safe event bus**
(`events.TypedEventBus`).

## Learning objectives
- Write generic types and methods with correct bounds and wildcards; explain PECS with a payments example.
- Explain erasure and its consequences (no `new T[]`, no `instanceof List<String>`, heap pollution, type tokens).
- Use phantom type parameters to encode domain rules (currency, id kind) at zero runtime cost.
- Know where static typing stops (data from DB/JSON) and add runtime checks at that boundary.

## Prerequisites
- Module01 and module02 (value objects). Sealed types and records (module07 covers them in depth).

## Java version notes
| Feature | Status | Where |
|---|---|---|
| Generics, wildcards, bounded types | Java 5+ | everywhere |
| Sealed interfaces, records with type parameters | **Java 17** | `result.Result`, `currency.*` |
| Record patterns over generic records (`case Ok<T,E>(var v)`) | **Java 21** final (preview in 19/20) | not used: module compiled with `--release 17` |

## Why generics matter in fintech code
Type safety is cheap defence: `TypedId<Account>` can never be passed where `TypedId<Wallet>` is expected,
`Result<Receipt, TransferError>` forces callers to handle failure, and `Repository<T, ID>` is written once.

## Topics and where to find them
| Topic | Package / class | Takeaway |
|---|---|---|
| Generic classes | `classes.Pair<A,B>`, `classes.TypedId<T>` | phantom type parameter gives compile-time distinction with zero runtime cost |
| Generic methods | `methods.CollectionUtils` | `<T> List<T> filter(Collection<? extends T>, Predicate<? super T>)` |
| Bounds | `bounds.Bounds` | `T extends Summable<T> & Comparable<T>`; `T extends Comparable<? super T>`; recursive `B extends Builder<B>` |
| Wildcards (PECS) | `wildcards.Wildcards` | `? extends` to read, `? super` to write, `?` for Object-only |
| Erasure | `erasure.Erasure` | heap pollution, type tokens, `Class<T>` array creation, bridge methods |
| Repository | `repository.*` | `Repository<T extends Entity<ID>, ID>` + `InMemoryRepository` + `PaymentRepository` specialisation |
| Result | `result.Result<T,E>` | sealed `Ok`/`Err`, `map`/`flatMap`/`mapErr`, exhaustive `switch` |

## BAD vs GOOD
```java
// BAD: raw types - compiles with warnings, fails at the read site much later
List raw = strings; raw.add(42);
String s = strings.get(0); // ClassCastException here, not where the bug was introduced
```
```java
// GOOD: type token survives erasure
public <T> T get(Class<T> type) { return type.cast(values.get(type)); }
```
```java
// BAD: invariant parameter rejects List<CardTx>
BigDecimal total(List<Transaction> txs)
// GOOD: producer-extends accepts any subtype list
BigDecimal total(Collection<? extends Transaction> txs)
```
```java
// BAD: exceptions for expected business outcomes
throw new InsufficientFundsException();
// GOOD: typed result the caller must handle
Result<Receipt, TransferError> r = svc.transfer(from, to, amount);
```

## Key Takeaways
- `List<CardTx>` is **not** a `List<Transaction>` (invariance). Arrays are covariant and therefore unsafe.
- PECS: Producer `extends`, Consumer `super`. Return types rarely need wildcards.
- `T extends Comparable<? super T>` is the correct bound for sortable things with inheritance.
- Erasure means no `new T()`, no `T[]`, no `instanceof T`, and `List<String>.class` does not exist. Use `Class<T>` tokens.
- A `@SuppressWarnings("unchecked")` must come with a comment proving why the cast is safe.
- Prefer `Result<T,E>` for expected failures and exceptions for programming errors / infrastructure faults.

## Cheat Sheet
| Syntax | Meaning |
|---|---|
| `<T>` | unbounded type parameter |
| `<T extends Number>` | upper bound |
| `<T extends A & B>` | multiple bounds (class first) |
| `<T extends Comparable<? super T>>` | comparable to itself or a supertype |
| `List<? extends T>` | read T, write nothing |
| `List<? super T>` | write T, read Object |
| `List<?>` | read Object, write nothing |
| `Class<T>` | type token |
| `B extends Builder<B>` | self type / recursive bound |

## 10 Interview Q&A
1. **What is type erasure?** The compiler removes type parameters and inserts casts; at runtime `List<String>` is `List`. Keeps backward compatibility.
2. **Why can't you create `new T[]`?** Erasure: the runtime does not know T. Use `Array.newInstance(type, n)` with a `Class<T>` token.
3. **Explain PECS.** `? extends` for producers you read from, `? super` for consumers you write into. `Collections.copy(List<? super T> dest, List<? extends T> src)`.
4. **Why is `List<Integer>` not a `List<Number>`?** Generics are invariant; otherwise you could add a `Double` to a `List<Integer>` through the `List<Number>` view.
5. **What is heap pollution?** A variable of a parameterised type refers to an object that is not of that type, usually via raw types or unchecked casts; `ClassCastException` appears far from the cause.
6. **What are bridge methods?** Synthetic methods the compiler generates so erased overrides still override, e.g. `compareTo(Object)` delegating to `compareTo(Money)`.
7. **When would you use a recursive bound?** Fluent builders (`B extends Builder<B>`) and `Enum<E extends Enum<E>>` so that methods return the concrete subtype.
8. **Can you overload `f(List<String>)` and `f(List<Integer>)`?** No, both erase to `f(List)`.
9. **`Result` vs exceptions?** Result for expected, recoverable business outcomes you want callers to handle explicitly; exceptions for bugs and infrastructure failures.
10. **How does `Class<T>.cast` help a heterogeneous container?** The token both keys the map and provides a runtime-checked cast, so the container is type-safe without unchecked warnings.

## Exercises for the reader
1. Add `Result.sequence(List<Result<T,E>>) : Result<List<T>,E>` that short-circuits on the first error.
2. Write `Repository.findPage(int offset, int limit, Comparator<? super T>)` and prove ordering with a test.
3. Make `TypedId` implement `Comparable<TypedId<T>>` and sort a `List<TypedId<Account>>`.

---

## Production deep dive

### Example 1: phantom-typed currencies (`module05.currency`)
```java
Amount<EUR> fee = Amount.of("1.50", EUR.INSTANCE);
Amount<USD> usd = Amount.of("10.00", USD.INSTANCE);
fee.plus(usd);                                   // compile error
FxRate<EUR, USD> eurUsd = ...;
eurUsd.convert(usd);                             // compile error: needs Amount<EUR>
FxRate<EUR, JPY> cross = eurUsd.then(usdJpy);    // middle currency must line up, checked by javac
```
**Why it matters.** Currency-mixing bugs are silent: `10 EUR + 10 USD = 20` passes every unit test that uses a single currency.
A runtime check (module01/02 `Money`) catches them in production; a type parameter catches them in the IDE.
**Expected behaviour.** Scale is enforced per currency (`JPY` has 0 decimals, so `1.5 JPY` throws). Conversion rounds
`HALF_EVEN` exactly once, at the end. `inverse()` keeps 10 dp so a round trip drifts by at most one minor unit (tested).
**Limitation and boundary.** Erasure removes `C` at runtime, so amounts read from DB/JSON are `Amount<?>` and must be narrowed with
`Amount.as(untyped, EUR.INSTANCE)`, a runtime-checked cast. That is why `unit` is also a real component.
**Trade-off.** Strong typing is great for code paths with a fixed currency (fee schedules, domestic rails). Multi-currency
wallets, where the currency is data, still need runtime-typed `Money`. Don't force phantom types where the currency is dynamic.

### Example 2: `events.TypedEventBus`, a type-safe heterogeneous container
```java
<E extends B> void subscribe(Class<E> type, Consumer<? super E> handler)
```
- `Class<E>` is a **type token**: it survives erasure and ties the key to the handler's type.
- `Consumer<? super E>` is **PECS** (consumer super): a generic audit handler `Consumer<PaymentEvent>` can subscribe to `PaymentSettled`.
- Dispatch walks the class hierarchy, so a handler on the root type sees every event (the audit trail).
- **Failure isolation:** a throwing notifier does not prevent the audit handler from running. Failures are returned, not swallowed.

**In-memory vs production.** This bus is synchronous and process-local. If the JVM dies after the DB commit but before
`publish`, the event is lost. Cross-service events go through a transactional outbox to a broker (module15 `P16`),
and consumers must deduplicate because brokers deliver at least once.

## Generics decision table
| Situation | Signature |
|---|---|
| Read items of T from a collection | `Collection<? extends T>` |
| Write T into a collection | `Collection<? super T>` |
| Both read and write | `Collection<T>` |
| Comparator / predicate parameter | `Comparator<? super T>`, `Predicate<? super T>` |
| Return type | concrete `T` / `List<T>`, never a wildcard |
| Need the class at runtime | pass `Class<T>` (type token) |
| Self-typed builder | `B extends Builder<B>` |

## Common pitfalls
- Raw types (`List`, `Repository`) silence the compiler and move the `ClassCastException` to an unrelated line.
- `@SuppressWarnings("unchecked")` on a whole class instead of the one proven-safe line, with a comment saying why.
- Returning wildcard types (`List<? extends Payment>`), which forces every caller to deal with capture.
- Assuming generic type information exists at runtime (`instanceof Amount<EUR>` is illegal).
- Generic varargs (`List<T>...`) without `@SafeVarargs`, which risks heap pollution.
- Over-engineering: a five-type-parameter abstraction nobody can read is worse than two concrete classes.

## Production checklist
- [ ] No raw types; compiler warnings treated as errors in CI (`-Xlint:all -Werror` where practical).
- [ ] Every unchecked cast is isolated, commented and justified.
- [ ] Domain distinctions that must never mix (currencies, id kinds) are encoded in types where they are static.
- [ ] Runtime checks exist at the deserialisation boundary for anything typed only by convention.
