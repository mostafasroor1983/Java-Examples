# Module 07 - Modern Java (17 and 21 language features)

Package root: `module07`. Domain: payment requests, payment results, payouts, fee tiers.
The POM sets `maven.compiler.source/target` properties to 17 but the compiler plugin overrides them to 21, so this module
also uses record patterns and pattern-matching `switch`, which became final in Java 21.

## Features and where to find them
| Feature | Since | Class | Fintech use |
|---|---|---|---|
| Records | 16 | `records.Money`, `records.PaymentRequest` | immutable DTOs/value objects, compact-constructor validation, local records in streams |
| Sealed types | 17 | `sealed.PaymentResult` | closed set of outcomes, exhaustive handling |
| `instanceof` patterns | 16 | `patterns.PatternMatching#routeInstanceof` | no cast boilerplate, flow scoping |
| Record patterns + `switch` patterns | 21 | `patterns.PatternMatching#describe`, `sealed.ResultHandler` | nested deconstruction, guards (`when`), `case null` |
| Switch expressions | 14 | `switches.SwitchExpressions` | no fall-through, exhaustive enums, `yield` |
| Text blocks | 15 | `textblocks.TextBlocks` | JSON/SQL templates, `formatted`, `\` and `\s` escapes |
| Java 8 vs 17 | - | `java8vs17.Java8Vs17` | `List.of`, `var`, `Optional.or`, `takeWhile`, `toList`, `String.lines/strip/repeat`, records vs classes |

## BAD vs GOOD
```java
// Java 8: cast after instanceof, silent fall-through on missing break
if (o instanceof Card) { Card c = (Card) o; ... }
switch (tier) { case STANDARD: n = 1; /* forgot break */ case PLUS: n = 3; break; }
```
```java
// Java 17/21
if (o instanceof Card c && c.scheme().equals("AMEX")) ...
int n = switch (tier) { case STANDARD -> 1; case PLUS -> 3; case PREMIUM, METAL -> 10; };
```
```java
// instanceof chain with default that hides a missing case
if (r instanceof Success) ... else if (r instanceof Declined) ... else return "unknown";
// exhaustive switch over a sealed type: adding a subtype is a compile error here
return switch (r) { case Success s -> ...; case Declined d -> ...; case Pending p -> ...; case Failed f -> ...; };
```

## Records: what interviewers probe
- Compact constructor assigns to *parameters*; the fields are assigned afterwards. Normalise/validate there.
- Records are shallowly immutable: `List.copyOf` your collection components.
- You can add static factories, instance methods, nested records, implement interfaces; you cannot add instance fields or extend a class.
- An accessor override must return the component type exactly; expose an `Optional` view under another name (`optionalReference()`).
- Records are implicitly `final` and have a canonical constructor; `equals` compares components (BigDecimal scale matters!).

## Sealed + switch: exhaustiveness rules
- Selector is a sealed interface/enum -> no `default` required; compiler checks all permitted subtypes.
- Guards (`when`) do not count toward exhaustiveness; an unguarded case for the same type must follow.
- `case null` must be explicit or the switch throws `NullPointerException`.
- Dominance: a more specific pattern must come before a more general one, or compilation fails.

## Key Takeaways
- Records + sealed interfaces = algebraic data types; pair them with switch patterns for safe domain modelling.
- Avoid `default` in switches over sealed/enum types so new cases fail at compile time.
- Text blocks keep SQL/JSON readable; still use parameters, never string-concatenate user input.
- `List.of`/`Map.of` are immutable and reject nulls; `Arrays.asList` is fixed-size but mutable in place.
- `var` is for local inference; keep it where the right-hand side makes the type obvious.
- `Stream.toList()` returns an unmodifiable list; `Collectors.toList()` makes no such promise.

## Cheat Sheet
| Idiom | Java 8 | Java 17/21 |
|---|---|---|
| immutable list | `unmodifiableList(new ArrayList<>(...))` | `List.of(...)` |
| data class | 40 lines | `record` |
| type test | `instanceof` + cast | `instanceof T t` / `case T t ->` |
| closed hierarchy | docs + `default` | `sealed ... permits` |
| multi-line string | `+ "\n" +` | `"""` text block |
| conditional switch | statement + `break` | expression + `->` + `yield` |
| optional fallback | `isPresent()/get()` | `or()`, `orElseThrow()`, `ifPresentOrElse()` |

## 10 Interview Q&A
1. **Can a record have a mutable field?** No instance fields beyond components; components are `final`. The referenced object may still be mutable, so copy collections.
2. **Difference between compact and canonical constructor?** Compact omits the parameter list and assigns fields implicitly after the body; canonical is explicit. Use compact for validation/normalisation.
3. **Why sealed instead of an enum?** Variants can carry different data (`Success` has an auth code, `Declined` a reason); enums share one shape.
4. **What does `permits` do if omitted?** Permitted subclasses are inferred from the same compilation unit (file).
5. **Can a sealed interface have non-final implementations?** Yes: `final`, `sealed`, or `non-sealed` must be declared on each permitted subtype; records are implicitly final.
6. **What does a switch do with `null`?** Throws NPE unless `case null` (or `case null, default`) is present.
7. **Why can't you fall through in arrow switches?** Arrow form has no fall-through by design; use `case A, B ->` for shared bodies.
8. **How does a text block treat indentation?** Common leading whitespace (relative to the closing delimiter and least-indented line) is removed; trailing spaces are stripped unless escaped with `\s`.
9. **`List.of()` vs `Arrays.asList()`?** `List.of` is fully immutable and null-hostile; `Arrays.asList` is a fixed-size view over the array that allows `set`.
10. **Is `var` dynamic typing?** No, it is compile-time inference; the variable has exactly one static type.

## Exercises for the reader
1. Add `PaymentResult.Reversed(paymentId, originalAuthCode)` and fix every compile error the compiler reports.
2. Write `Money.allocate(int parts)` that splits a sum without losing pennies (largest remainder) and test with 100.00 / 3.
3. Convert `PatternMatching.routeOld` callers to a single switch with record patterns and a `case null`.
