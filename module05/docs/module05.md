# Module 05 - Generics

Package root: `module05`. Domain: typed IDs, repositories, transfer results.

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
