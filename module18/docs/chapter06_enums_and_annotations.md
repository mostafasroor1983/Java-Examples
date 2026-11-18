# Chapter 6 - Enums and annotations (items 34-41)

Package `module18.ch06`, tests in `Chapter06Test`. Theme: let the type system carry domain constants, sets, maps and
metadata instead of ints, bit masks, ordinals and naming conventions.

| Instead of | Use | Item |
|---|---|---|
| `static final int` constants | `enum` with fields and behaviour | 34 |
| `ordinal()` as data | instance field | 35 |
| bit fields | `EnumSet` | 36 |
| arrays indexed by `ordinal()` | `EnumMap` | 37 |
| "extending" an enum | enum implementing an interface | 38 |
| naming patterns | annotations | 39 |
| hoping a method overrides | `@Override` | 40 |
| marker annotation for a type constraint | marker interface | 41 |

---

## Item 34 - Use enums instead of int constants
**In short.** Enums are type-safe, namespaced, printable classes that can carry data and constant-specific behaviour.
**Example.** `CardScheme` holds a PAN prefix and interchange rate. `AMEX` overrides `interchange` with a minimum fee, and
`fromPan` is an Optional-returning lookup built from a static map.
**Why it matters.** `int status = 3` in a payment table gets misread, renumbered or confused with another constant set.
**Java 17/21.** Pattern-matching `switch` over enums is exhaustive without `default` (as switch expressions since 14). Since
Java 21, enum constants can be qualified in `case` labels of a switch over a sealed interface.
**Pitfalls.** Persisting enums by `ordinal()` (item 35); persist the name or an explicit code.
**Proof.** `item34_enumsCarryDataAndBehaviour`.

## Item 35 - Use instance fields instead of ordinals
**In short.** `ordinal()` is a position, not data. Store business values in fields.
**Example.** `SettlementCycleBad.daysToSettle()` uses the ordinal. In `SettlementCycle`, adding `INSTANT` first changed
`T0.ordinal()` to 1 but kept `daysToSettle()` at 0.
**Why it matters.** Settlement days, priority levels and fee tiers derived from ordinals silently change when someone reorders constants.
**Proof.** `item35_ordinalIsNotBusinessData`.

## Item 36 - Use EnumSet instead of bit fields
**In short.** `EnumSet` has bit-vector performance with type safety, readable `toString`, and the full `Set` API.
**Example.** Operator and supervisor roles as `EnumSet<Permission>`. `violatesSeparationOfDuties` checks that nobody holds both
INITIATE and APPROVE (maker-checker, module01).
**Why it matters.** Permission bugs are security bugs. `[VIEW_BALANCES, INITIATE_PAYMENT]` in an audit log beats `3`.
**Java 17/21.** Unchanged. Wrap with `Collections.unmodifiableSet` (or `Set.copyOf`) when exposing it.
**Proof.** `item36_enumSetForPermissions`.

## Item 37 - Use EnumMap instead of ordinal indexing
**In short.** `EnumMap` is an array internally, but type-safe and readable. Use it for per-enum aggregates and nested
transition tables.
**Example.** `totalsByChannel` via `groupingBy(..., () -> new EnumMap<>(Channel.class), ...)`, and a
`Map<State, Map<Event, State>>` transition table.
**Why it matters.** Per-channel totals and payment state machines are core fintech structures. Ordinal-indexed arrays break on reorder.
**Proof.** `item37_enumMapGroupingAndTransitions`.

## Item 38 - Emulate extensible enums with interfaces
**In short.** Enums cannot be subclassed, but they can implement an interface, so other enums can add new "constants".
**Example.** `BasicFeeRule` and `PromoFeeRule` both implement `FeeRule`. `cheapest` works on any enum class implementing the
interface (`<T extends Enum<T> & FeeRule>`) or on any collection of rules.
**Why it matters.** A core library ships basic rules; a partner team adds promotions without editing the core enum.
**Pitfalls.** Shared behaviour between enum implementations cannot be inherited; use default methods or helpers.
**Proof.** `item38_interfaceExtendsEnums`.

## Item 39 - Prefer annotations to naming patterns
**In short.** Naming conventions ("fields starting with secret") fail silently and cannot carry parameters. Annotations are
compiler-checked, targeted and parameterised.
**Example.** `@Sensitive(keepLast = 4)` on record components, plus a redactor using the Java 16+ `RecordComponent` reflection
API: `CardHolder[name=Jane, pan=************1111, cvv=***, country=DE]`.
**Why it matters.** Systematic redaction of PANs/CVVs in logs (PCI-DSS) driven by metadata on the data model.
**Java 17/21.** `ElementType.RECORD_COMPONENT` (16) propagates annotations to the field, accessor and constructor parameter as targeted.
**Proof.** `item39_annotationDrivenRedaction`.

## Item 40 - Consistently use the Override annotation
**In short.** Put `@Override` on every method that should override. The compiler then catches overloads that look like overrides.
**Example.** `OverloadedPaymentId.equals(OverloadedPaymentId)` overloads instead of overriding, so a HashSet keeps two equal ids.
`@Override` on it would not compile.
**Why it matters.** Duplicate payment ids slipping through deduplication.
**Java 17/21.** Records generate equals correctly; hand-written classes still need `@Override`.
**Proof.** `item40_overloadedEqualsLetsDuplicatesThrough`.

## Item 41 - Use marker interfaces to define types
**In short.** A marker interface defines a type, so misuse is a compile error. Marker annotations fit non-type elements and
annotation-driven frameworks.
**Example.** `Vault.storeInVault(PciScoped data)`: card data compiles; `Address` does not.
**Why it matters.** Keeping PCI data inside its compliant boundary enforced by the compiler, not by convention.
**Java 17/21.** A **sealed** marker interface can additionally restrict which types may be PCI-scoped.
**Proof.** `item41_markerInterfaceIsCompileTimeChecked`.

---
**Say it in an interview.** "Enums with fields and behaviour, never ordinals; EnumSet for permissions, EnumMap for per-enum
data and state machines; annotations for metadata like `@Sensitive`; marker interfaces when the compiler should enforce the constraint."
