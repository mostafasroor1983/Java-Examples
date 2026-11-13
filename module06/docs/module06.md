# Module 06 - Streams and Functional Programming

Package root: `module06`. Domain: transaction statements, risk reports, FX conversion batches.

## Mental model
A stream is a lazy pipeline: *source -> intermediate ops -> terminal op*. Elements flow **one at a time**
through all stages (vertical processing) until a stateful barrier (`sorted`, `distinct`) or the end.
Nothing runs without a terminal operation, and a stream can be consumed once.

## Map of the module
| Package | Contents |
|---|---|
| `basics` | `filter/map/flatMap/sorted/reduce/max/anyMatch`, `IntStream`, top-N by grouped volume |
| `collectors` | `groupingBy` (with downstream + map factory), `partitioningBy`, `toMap` with/without merge, `joining`, `teeing`, nested grouping, custom `summingBigDecimal` collector |
| `lazy` | execution trace proving laziness, infinite `Stream.iterate` + `limit`, `sorted` barrier, single-use |
| `parallel` | associativity requirement, shared-state bug, `collect` vs `forEach`, dedicated `ForkJoinPool` |
| `pitfalls` | `double` for money, `toMap` NPE on null values, `Optional.get`, side effects elided by `count()` |
| `exercise1` | `RiskReport` per account with flagging |
| `exercise2` | `AccountSummary` monthly statement |
| `exercise3` | `FxConversionResult` batch conversion with partial failure |

## BAD vs GOOD
```java
// BAD: throws IllegalStateException on the second transaction for account A
txs.stream().collect(Collectors.toMap(Transaction::accountId, Transaction::amount));
// GOOD
txs.stream().collect(Collectors.toMap(Transaction::accountId, Transaction::amount, BigDecimal::add));
```
```java
// BAD: shared mutable state from parallel forEach -> lost elements / AIOOBE
List<Integer> out = new ArrayList<>(); range(0, n).parallel().forEach(out::add);
// GOOD
range(0, n).parallel().boxed().collect(toList());
```
```java
// BAD: 0.1 + 0.2 = 0.30000000000000004
amounts.stream().mapToDouble(Double::parseDouble).sum();
// GOOD
amounts.stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);
```
```java
// BAD: since Java 9, count() may skip map() entirely when the size is known
values.stream().map(v -> { counter++; return v; }).count();
```

## Parallel streams: when
Yes: large in-memory `ArrayList`/array/range, CPU-bound, stateless, associative reduction, no I/O, you measured.
No: small collections, I/O or blocking calls (starves the common pool), `LinkedList`/`Iterator` sources, order-dependent logic,
anything in a latency-sensitive request path shared with other code using the common pool.

## Key Takeaways
- Streams are lazy; `peek` for debugging only; side effects belong in `forEach`/`collect`.
- `reduce` needs an identity and an associative, stateless operation. `BigDecimal::add` is fine; `(a,b) -> a-b` is not.
- `toMap` throws on duplicate keys and NPEs on null values; always pass a merge function.
- `groupingBy` with a downstream collector replaces most manual map-building.
- `Collectors.teeing` does two aggregations in one pass.
- Use `Optional.orElseThrow(Supplier)` with a message, never bare `get()`.
- Loops are fine. Streams are for declarative transformations, not for everything.

## Cheat Sheet
| Need | Stream idiom |
|---|---|
| sum money | `.map(Tx::amount).reduce(ZERO, BigDecimal::add)` |
| group + sum | `groupingBy(key, reducing(ZERO, mapper, BigDecimal::add))` |
| group + count | `groupingBy(key, counting())` |
| two buckets | `partitioningBy(predicate)` |
| key -> value | `toMap(k, v, merge)` |
| top-N | `.sorted(cmp.reversed()).limit(n)` |
| first match | `.filter(p).findFirst()` |
| flatten | `.flatMap(List::stream)` |
| avoid boxing | `IntStream`/`LongStream`/`mapToInt` |
| CSV | `.collect(joining(",", "[", "]"))` |

## 10 Interview Q&A
1. **Intermediate vs terminal operations?** Intermediate return a stream and are lazy (`filter`, `map`, `sorted`); terminal trigger execution (`collect`, `reduce`, `forEach`, `count`).
2. **What is a stateful intermediate op?** One that needs to see all/previous elements: `sorted`, `distinct`, `limit`, `skip`. They break pure vertical processing.
3. **Why must `reduce`'s operator be associative?** Parallel execution splits the source and combines partial results in an arbitrary tree; non-associative ops give different answers.
4. **Which thread pool do parallel streams use?** The common `ForkJoinPool` (parallelism = cores - 1). Blocking in it starves every other parallel stream in the JVM.
5. **How do you sum `BigDecimal` with a collector?** There is no `summingBigDecimal`; use `reducing(ZERO, mapper, BigDecimal::add)` or a custom `Collector.of`.
6. **What does `Collectors.toMap` do on duplicate keys?** Throws `IllegalStateException` unless you provide a merge function.
7. **`findFirst` vs `findAny`?** `findFirst` respects encounter order (slower in parallel); `findAny` returns any element.
8. **Why avoid side effects in lambdas?** The runtime may reorder, elide (`count()`), or run them concurrently; results become non-deterministic.
9. **`Stream.iterate` vs `Stream.generate`?** `iterate` builds each element from the previous (needs `limit`/`takeWhile`); `generate` uses a supplier.
10. **When is a `for` loop better?** Index-based logic, early exit with complex state, hot paths where allocation matters, or when the stream version is harder to read.

## Exercises for the reader
1. Add `RiskReport.byCurrency` producing `Map<String, List<AccountRisk>>` with `groupingBy` + downstream `collectingAndThen`.
2. Implement `AccountSummary.dailyRunningBalance` returning a `TreeMap<LocalDate, BigDecimal>` using a sequential scan (why not a stream?).
3. Make `FxConversionResult.convertAll` parallel and prove with a test that totals are identical to the sequential run.
