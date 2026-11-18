# Chapter 3 - Methods common to all objects (items 10-14)

Package `module18.ch03`, tests in `Chapter03Test`. Theme: the contracts of `equals`, `hashCode`, `toString`, `clone` and
`compareTo`, which every collection, cache, log line and sort depends on.

| Method | Contract | Broken -> symptom |
|---|---|---|
| `equals` | reflexive, symmetric, transitive, consistent, non-null | duplicates in sets, failed lookups |
| `hashCode` | equal objects have equal hashes | `HashSet.contains(copy) == false` |
| `toString` | informative, documented format, **no secrets** | useless logs, or PCI/GDPR breaches |
| `clone` | copy is independent | shared mutable arrays between "copies" |
| `compareTo` | sign-consistent, transitive, ideally consistent with equals | wrong sort order, `TreeSet` drops elements |

---

## Item 10 - Obey the general contract when overriding equals
**In short.** Override `equals` only for value classes, and then satisfy all five properties. The most common violation is
asymmetry from trying to be equal to objects of another type.
**Example.** `BrokenCaseInsensitiveRef.equals("inv-1")` is true but `"inv-1".equals(ref)` is false. `PaymentReference` (record)
normalises once in the compact constructor and compares only with its own type.
**Why it matters.** Deduplication of payment references, idempotency keys and reconciliation matching all rely on equals.
**Java 17/21.** **Records** generate a correct `equals` from all components. `instanceof` patterns (`o instanceof Money m`)
make hand-written equals shorter. Subclassing a value class and adding fields still cannot preserve the contract; prefer
composition.
**Pitfalls.** `BigDecimal` equality is scale-sensitive (`1.0` != `1.00`), so normalise. Comparing mutable fields. Overloading
instead of overriding (item 40).
**Proof.** `item10_symmetryBrokenVsFixed`.

## Item 11 - Always override hashCode when you override equals
**In short.** Equal objects must produce equal hash codes. Use exactly the fields used by equals. Immutable classes may
cache the hash lazily.
**Example.** `BrokenAccountKey` (no hashCode) is not found in a `HashSet` by an equal key. `AccountKey` caches its hash (a benign race).
**Why it matters.** A broken key lets the same payment through twice, or loses cache entries.
**Java 17/21.** Records generate it. `Objects.hash(...)` is convenient but allocates a varargs array, so hand-roll it on hot paths.
**Pitfalls.** Excluding significant fields "for speed"; hash codes of mutable fields used as map keys (module02).
**Proof.** `item11_hashSetFindsEqualKeyOnlyWithHashCode`.

## Item 12 - Always override toString
**In short.** Return a concise, informative representation including the interesting state, and document whether the format
is stable. Provide accessors for everything in it.
**Example.** `Transfer` prints the reference, **masked** IBANs and a plain-string amount (`1E+2` becomes `100`).
**Why it matters (fintech twist).** toString output lands in logs, traces and exception messages, so it must never contain PANs,
full IBANs, tokens or secrets.
**Java 17/21.** A record's default toString prints every component. Override it when a component is sensitive (see also item
39's annotation-driven redaction).
**Pitfalls.** Making clients parse toString output, or changing a documented format.
**Proof.** `item12_informativeAndSafeToString`.

## Item 13 - Override clone judiciously
**In short.** `Cloneable` is a flawed design: shallow by default, bypasses constructors, conflicts with final fields. Prefer a
copy constructor or static copy factory. Arrays are the exception: `array.clone()` is idiomatic.
**Example.** `ExposureLimits.clone()` deep-copies its array (otherwise both copies share the limits). `copyOf(other)` is the
preferred alternative. The test mutates the original and checks that both copies are unaffected.
**Why it matters.** A "copy" that shares a mutable limits array changes a customer's exposure limits through another reference.
**Java 17/21.** Unchanged. Immutable types and records remove the need for copying altogether.
**Pitfalls.** Calling overridable methods from clone; declaring `throws CloneNotSupportedException` on a public clone.
**Proof.** `item13_deepCopyVsShallow`.

## Item 14 - Consider implementing Comparable
**In short.** Value types with a natural order should implement Comparable. Build comparators with `Comparator.comparing(...)
.thenComparing(...)`, and never compare by subtraction.
**Example.** `Transaction` orders by value date, then amount descending, then id. `BROKEN_BY_SUBTRACTION` returns the wrong sign
for 3,000,000,000 cents vs 0, and claims `2^32` equals `0`.
**Why it matters.** Statement ordering, FIFO allocation of payments and top-N reports depend on correct comparators.
**Java 17/21.** `Comparator` combinators (Java 8) remain the idiom. Note that `BigDecimal.compareTo` is inconsistent with
`equals`, so a `TreeSet<BigDecimal>` and a `HashSet<BigDecimal>` disagree about duplicates.
**Pitfalls.** Comparators that are not transitive; using `compareTo == 0` and `equals` interchangeably for BigDecimal.
**Proof.** `item14_naturalOrderAndOverflow`.

---
**Say it in an interview.** "For value types I use records, normalise state in the compact constructor, override toString
when a component is sensitive, avoid clone in favour of copy factories, and build comparators with `Comparator.comparing`.
Never subtraction."
