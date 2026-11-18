# Chapter 5 - Generics (items 26-33)

Package `module18.ch05`, tests in `Chapter05Test`. Theme: move type errors from runtime (ClassCastException in production)
to compile time. Background: module05.

| Concept | One line |
|---|---|
| Invariance | `List<CardPayment>` is not a `List<Payment>` |
| Erasure | type arguments vanish at runtime, so no `new T[]`, no `instanceof List<String>` |
| PECS | producer `extends`, consumer `super` |
| Heap pollution | a variable of a parameterised type refers to an object of another type |

---

## Item 26 - Don't use raw types
**In short.** Raw types compile with only a warning and defer failures to a distant read site. Use parameterised types, or
`List<?>` when the element type does not matter.
**Example.** `addLegacyEntry(List raw)` inserts `42` into a `List<String>`; `firstReference` later throws `ClassCastException`.
`countDistinct(List<?>)` is the safe "any type" API.
**Why it matters.** The failure surfaces in an unrelated report job, far from the integration code that caused it.
**Java 17/21.** Unchanged; raw types remain only for legacy compatibility, class literals and `instanceof`.
**Proof.** `item26_rawTypeFailsFarFromTheCause`.

## Item 27 - Eliminate unchecked warnings
**In short.** Every unchecked warning is a potential ClassCastException. Remove what you can; suppress only provably safe ones,
on the narrowest scope (a local variable), with a comment explaining why.
**Example.** `Buffer.get` suppresses on a single local variable declaration, with the safety argument written next to it.
**Java 17/21.** Unchanged. Compile with `-Xlint:all` (and ideally `-Werror`) in CI.
**Pitfalls.** `@SuppressWarnings("unchecked")` on a whole class hides future real problems.
**Proof.** `item27_narrowSuppressionStillTypeSafe`.

## Item 28 - Prefer lists to arrays
**In short.** Arrays are covariant and reified (errors at runtime); generics are invariant and erased (errors at compile time).
Mixing them produces warnings and errors, so prefer `List<E>`.
**Example.** `Object[] amounts = new String[1]; amounts[0] = 42;` throws `ArrayStoreException`. `Chooser<T>` picks a random
provider from a `List` using the Java 17 `RandomGenerator` API.
**Java 17/21.** `RandomGenerator`/`RandomGeneratorFactory` (JEP 356, Java 17) give pluggable, reproducible generators.
**Pitfalls.** Generic varargs (item 32) are arrays in disguise.
**Proof.** `item28_arraysFailAtRuntimeListsAtCompileTime`.

## Item 29 - Favor generic types
**In short.** Containers written for `Object` force casts on callers and accept wrong types. Make them generic.
**Example.** `BoundedQueue<E>`: bounded FIFO with `offer` returning false when full (back-pressure) and `Optional` poll.
**Java 17/21.** Records can be generic (`record Pair<A, B>(A a, B b)`).
**Proof.** `item29_genericBoundedQueue`.

## Item 30 - Favor generic methods
**In short.** Static utilities should be generic for inference and type safety. Use recursive bounds for comparable types,
and a generic singleton factory for stateless function objects.
**Example.** `union(Set<? extends E>, Set<? extends E>)`, `max(Collection<? extends E>)` with
`<E extends Comparable<? super E>>`, and `identity()`.
**Proof.** `item30_genericMethodsInferTypes`.

## Item 31 - Use bounded wildcards to increase API flexibility
**In short.** PECS: a parameter you read from is `? extends E`; one you write to is `? super E`. Never use wildcards in return types.
**Example.** `Buffer<Payment>.pushAll(List<CardPayment>)` and `drainTo(List<Object>)` both compile only thanks to wildcards.
**Why it matters.** Libraries like ledgers and event buses are used with many subtypes; rigid signatures force callers to copy collections.
**Proof.** `item31_pecs`.

## Item 32 - Combine generics and varargs judiciously
**In short.** Generic varargs create an array of a non-reifiable type. It is safe only if the method never writes to it and never
lets it escape; then annotate with `@SafeVarargs`.
**Example.** `flatten(List<? extends T>...)` is safe. `toArrayUnsafe` returns the array, so `pickTwoStrings` throws
`ClassCastException` (Object[] is not String[]).
**Java 17/21.** `@SafeVarargs` is allowed on private methods since Java 9.
**Proof.** `item32_safeVarargsVsExposedArray`.

## Item 33 - Consider typesafe heterogeneous containers
**In short.** Parameterise the key (`Class<T>`) instead of the container, so one container can safely hold values of
different types. `Class.cast` adds a runtime check.
**Example.** `PaymentContext.put(String.class, "trace-42")`, `get(Integer.class)`. A raw-typed attempt to store a String
under `Integer.class` throws.
**Why it matters.** Request contexts carry trace ids, risk scores and tiers without unchecked `Map<String, Object>` casting.
**Pitfalls.** Cannot use non-reifiable keys (`List<String>.class`); consider super type tokens or dedicated key objects.
**Proof.** `item33_typesafeHeterogeneousContainer`.

---
**Say it in an interview.** "No raw types, warnings treated as errors, lists over arrays, PECS for parameters, `@SafeVarargs`
only when the array never escapes, and `Class<T>` keys for heterogeneous containers."
