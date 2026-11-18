# Chapter 9 - General programming (items 57-68)

Package `module18.ch09`, tests in `Chapter09Test`. Theme: everyday code quality: scoping, loops, libraries, numeric types,
strings, interfaces, reflection, native code, optimisation and naming. Item 60 is the single most important one for fintech.

---

## Item 57 - Minimize the scope of local variables
**In short.** Declare variables where first used, initialised. Prefer `for` loops (loop variable scoped to the loop) to `while`.
Keep methods small.
**Example.** `copyPasteBug` reuses the first iterator in the second `while` loop, so the failed-payments list is silently skipped
(2 processed instead of 4). With `for` loops the same mistake would not compile.
**Java 17/21.** `var` (Java 10) keeps declarations short; still initialise at declaration.
**Proof.** `item57_scopeBugSilentlySkipsWork`.

## Item 58 - Prefer for-each loops to traditional for loops
**In short.** for-each removes iterator/index variables, which is where bugs hide. Explicit iterators only for removal (prefer
`removeIf`), replacement (`replaceAll`), or parallel iteration.
**Example.** `pairsBuggy` calls the outer `next()` inside the inner loop, which throws `NoSuchElementException`; `pairs` is correct.
**Proof.** `item58_nestedIteratorBug`.

## Item 59 - Know and use the libraries
**In short.** The JDK has already solved, tested and optimised most utilities. Hand-rolled versions have subtle bugs.
**Example.** `Math.abs(Integer.MIN_VALUE) % n` is negative (bad shard index); `Math.floorMod` is correct. `RandomGenerator.nextInt(bound)`
is unbiased. `HexFormat` (17) formats digests.
**Java 17/21.** Useful newer APIs: `String.strip/isBlank/lines/repeat` (11), `Collectors.teeing` (12), `Stream.toList` (16),
`HexFormat`, `RandomGenerator` (17), sequenced collections (21), `Math.clamp` (21).
**Proof.** `item59_libraryBeatsHandRolled`.

## Item 60 - Avoid float and double if exact answers are required
**In short.** Binary floating point cannot represent most decimal fractions. Use `BigDecimal` (from Strings) or integer minor units.
**Example.** `1.03 - 0.42 == 0.6100000000000001`. Buying 0.10, 0.20, 0.30 ... items with 1.00 gives **3** items with double
(0.3999999999999999 left) and **4** with BigDecimal or cents.
**Why it matters.** Rounding errors accumulate across millions of postings and break reconciliation.
**Java 17/21.** Unchanged. See module02 (`BigDecimal` rules) and module14 (`long` minor units, property-tested).
**Proof.** `item60_doubleGivesWrongMoneyAnswers`.

## Item 61 - Prefer primitive types to boxed primitives
**In short.** Boxed values have identity (`==` compares references), can be null (unboxing NPE), and allocate.
**Example.** `BROKEN.compare(1000, 1000)` returns 1 (two different Integer objects); `isHighRisk()` unboxes a null `Integer` and
throws NPE.
**Why it matters.** Risk scores and limits stored as `Integer` fields default to null and crash the authorisation path.
**Java 17/21.** Value types (Project Valhalla) are not available in 17/21.
**Proof.** `item61_boxedPitfalls`.

## Item 62 - Avoid strings where other types are more appropriate
**In short.** Strings are poor substitutes for numbers, enums, aggregates and capabilities. Parse once into a typed value.
**Example.** `"EUR:100.00".split(":")` everywhere vs `Amount.parse` returning a record with `Currency` and `BigDecimal`.
**Proof.** `item62_typesInsteadOfCompoundStrings`.

## Item 63 - Beware the performance of string concatenation
**In short.** `+=` in a loop is quadratic. Use a pre-sized `StringBuilder` or `joining`. A single `a + b + c` expression is fine.
**Example.** `renderSlow` vs `render` produce identical statements; `render` is linear.
**Java 17/21.** Since Java 9, simple concatenation compiles to `invokedynamic` (JEP 280), but loops are still quadratic.
**Proof.** `item63_builderSameOutput`.

## Item 64 - Refer to objects by their interfaces
**In short.** Declare variables, parameters and returns with interfaces so implementations can change. Use classes only when
you need class-specific behaviour.
**Example.** **Java 21** `SequencedCollection` lets `firstAndLast` accept a List, a LinkedHashSet or an ArrayDeque.
**Java 17/21.** `SequencedCollection`, `SequencedSet`, `SequencedMap` (JEP 431, Java 21). On 17, accept `List` or `Deque`.
**Proof.** `item64_sequencedCollectionInterface`.

## Item 65 - Prefer interfaces to reflection
**In short.** Reflection loses compile-time checking and is slow and verbose. Use it only to instantiate a class known at
runtime, then use it through an interface; translate reflective exceptions into one meaningful exception.
**Example.** `load(className)` returns a `FeePolicy`; unknown classes and non-FeePolicy classes produce `IllegalArgumentException`.
**Java 17/21.** `ServiceLoader` (with JPMS `provides/uses`) is often the cleaner plugin mechanism. Strong encapsulation (Java 16/17)
blocks deep reflection into JDK internals.
**Proof.** `item65_reflectiveInstantiationInterfaceUse`.

## Item 66 - Use native methods judiciously
**In short.** JNI is unsafe (memory corruption crashes the JVM), non-portable, and hard to debug; the JVM is fast enough for
most work.
**Example.** Settlement-file checksum with `CRC32C` (pure Java, JIT-intrinsified) instead of a native library.
**Java 17/21.** The **Foreign Function & Memory API** replaces most JNI: incubator in 17, preview in 19-21, **final in Java 22**
(JEP 454). Not used here because the module avoids preview APIs.
**Proof.** `item66_pureJavaChecksum`.

## Item 67 - Optimize judiciously
**In short.** Write clear, well-designed code first; avoid API decisions that limit performance forever; measure before and after.
**Example.** `measure(..)` shows how to time a call, and is explicitly not a benchmark (no warm-up or forks; see module14 and JMH).
**Proof.** `item67_measureDontGuess`.

## Item 68 - Adhere to generally accepted naming conventions
**In short.** Typographical: packages lowercase, Types UpperCamel, members lowerCamel, CONSTANTS, type parameters `T E K V R X`.
Grammatical: `isX`/`hasX` booleans, `toX` conversions, `asX` views, `of/from/valueOf` factories.
**Example.** `AccountNumber.of`, `from(iban)`, `isDomestic()`, `toMaskedString()`, `MAX_DAILY_TRANSFERS`, `lookup<K, V>`.
**Proof.** `item68_conventionalNames`.

---
**Say it in an interview.** "Money is never double; no boxed fields that can be null; library methods like `floorMod` over
hand-rolled maths; types instead of compound strings; interfaces (including Java 21 `SequencedCollection`) in signatures;
measure before optimising."
