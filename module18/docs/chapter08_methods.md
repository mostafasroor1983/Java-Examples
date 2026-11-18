# Chapter 8 - Methods (items 49-56)

Package `module18.ch08`, tests in `Chapter08Test`. Theme: method contracts that are hard to misuse: validated inputs,
defensive copies, readable signatures, unsurprising overloads, no nulls, and documentation.

---

## Item 49 - Check parameters for validity
**In short.** Validate at the start of public methods and constructors and fail fast with the right exception (NPE via
`Objects.requireNonNull`, IAE, IOOBE via `Objects.checkIndex`). Document restrictions with `@throws`.
**Example.** `Transfer` record validates in its compact constructor; `nth` uses `Objects.checkIndex` (Java 9).
**Why it matters.** A negative amount that slips through becomes a credit instead of a debit, much later and far away.
**Java 17/21.** Records' compact constructors are the natural place. `Objects.checkFromToIndex`/`checkFromIndexSize` cover ranges.
**Pitfalls.** Validating after partially mutating state (see item 76); `assert` for public API checks (disabled by default).
**Proof.** `item49_failFastWithTheRightException`.

## Item 50 - Make defensive copies when needed
**In short.** If a class holds mutable objects from clients, copy them on the way in (before validating, to close the
check-then-use window) and on the way out. Better: use immutable types.
**Example.** `LeakyPeriod`: mutating the caller's `Date` after construction breaks "start <= end". `StatementPeriod` copies in and
out. `Period` uses `Instant` and needs no copies.
**Why it matters.** Statement periods, interest periods and settlement windows decide money; tampering is a correctness and security issue.
**Java 17/21.** `java.time` types and records with `List.copyOf` make this mostly free.
**Pitfalls.** Using `clone()` on a parameter whose class may be an untrusted subclass.
**Proof.** `item50_defensiveCopiesBlockTampering`.

## Item 51 - Design method signatures carefully
**In short.** Clear names; at most about four parameters; no long runs of same-typed parameters; interfaces as parameter types;
two-element enums instead of booleans; parameter objects for values that travel together.
**Example.** `scheduleBad(String, String, String, BigDecimal, boolean, boolean)` vs `schedule(PaymentSchedule)` with `Priority` and
`Notification` enums.
**Why it matters.** Swapping `from` and `to` (both Strings) compiles and moves money the wrong way.
**Java 17/21.** Records are ideal parameter objects.
**Proof.** `item51_parameterObjectAndEnumsReadClearly`.

## Item 52 - Use overloading judiciously
**In short.** Overload resolution is static (compile-time types), unlike overriding. Avoid overloads with the same number of
parameters; use different names instead.
**Example.** A `Collection<?>` holding a Set picks `classify(Collection)`. `List<Integer>.remove(1)` removes the element at index 1, not value 1.
**Why it matters.** Removing the wrong account id from a block list.
**Proof.** `item52_overloadsChosenAtCompileTime`.

## Item 53 - Use varargs judiciously
**In short.** Varargs allocate and accept zero arguments. When one argument is required, take it explicitly: `max(first, rest...)`.
Provide fixed-arity overloads for hot paths.
**Example.** `maxBad()` compiles with no arguments and throws at runtime; `max(BigDecimal first, BigDecimal... rest)` cannot be
called empty.
**Proof.** `item53_requiredFirstArgument`.

## Item 54 - Return empty collections or arrays, not nulls
**In short.** Null forces a check at every call site; one forgotten check is an NPE. Empty immutable collections are free.
**Example.** `historyOrNull` vs `history` (`getOrDefault(..., List.of())`), and a shared zero-length array constant.
**Proof.** `item54_emptyNotNull`.

## Item 55 - Return optionals judiciously
**In short.** `Optional` is a good return type for "may be absent". Not for collections, fields, parameters or map values; never
return null from an Optional method; use `OptionalInt/Long/Double` for primitives; prefer `orElseThrow()` to `get()`.
**Example.** `overdraftLimit(account)` returns Optional; `effectiveLimit` uses `orElse(ZERO)`; `largestCents` returns `OptionalLong`.
**Proof.** `item55_optionalAsReturnType`.

## Item 56 - Write doc comments for all exposed API elements
**In short.** Document the contract: preconditions, postconditions, side effects, thread safety, and every exception. Use
`{@code}`, `@implSpec` for subclass-relevant self-use, and a summary first sentence.
**Example.** `Item56DocComments.convert` documents parameters, rounding policy, return guarantees and exceptions, and includes a
Java 18 `{@snippet}` example.
**Java 17/21.** **`{@snippet}` (JEP 413, Java 18)** replaces `<pre>{@code ...}</pre>` and supports highlighting and external
validated snippets. `javadoc -Xdoclint` catches missing tags.
**Proof.** `item56_documentedContractHolds` (the documented example value is asserted).

---
**Say it in an interview.** "Validate first, copy mutable inputs, prefer parameter objects and enums to boolean soup, avoid
same-arity overloads, never return null collections, Optional only as a return type, and document every exception."
