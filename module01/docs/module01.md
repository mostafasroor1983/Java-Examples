# Module 01 - OOP and Clean Design (SOLID)

Package root: `module01`. Domain: payments, wallets, fees, fraud checks.

## Why this matters at Revolut-level interviews
Interviewers rarely ask "what is encapsulation". They hand you a `PaymentService` that does five things
and ask what you would change. Being able to *name* the smell (SRP violation, fat interface, hard-wired
dependency) and refactor it live is the skill under test.

## The four pillars, payments edition

| Pillar | Package | Example |
|---|---|---|
| Abstraction | `module01.abstraction` | `PaymentProcessor` template method; `CardPaymentProcessor` and `BankTransferProcessor` supply only `settle()` |
| Encapsulation | `module01.encapsulation` | `BankAccount` hides `balance`, exposes `deposit/withdraw`, returns defensive copies of history |
| Inheritance vs Composition | `module01.composition` | `FraudEngine` HAS-A list of `FraudCheck` rules rather than a subclass per rule |
| Polymorphism | `module01.polymorphism` | sealed `PaymentMethod`; subtype dispatch vs exhaustive `switch` pattern matching |

Immutability (`module01.immutability`) is the fifth pillar in fintech: `Money` normalises scale so
`10.0 GBP` equals `10.00 GBP`, and `Transfer` validates in its compact constructor.

## SOLID: BAD vs GOOD

### S - Single Responsibility
**BAD** `solid.srp.bad.PaymentService` validates, writes SQL strings, formats e-mails and logs.
```java
public void pay(String customerEmail, BigDecimal amount) {
    // validation ... persistence ... notification ... logging - four reasons to change
}
```
**GOOD** `solid.srp.good.PaymentService` orchestrates a `PaymentValidator`, a `PaymentRepository` and a `Notifier`.
Each can be tested and replaced independently.

### O - Open/Closed
**BAD** `solid.ocp.bad.FeeCalculator` has a `switch(type)` with a `default: return ZERO`.
A typo like `"CRAD"` yields a zero fee silently.
**GOOD** `solid.ocp.good.FeeCalculator` iterates `List<FeePolicy>`; adding a fee type = adding a class.
Unknown types throw.

### L - Liskov Substitution
**BAD** `FixedDepositAccount extends Account` but throws from `withdraw()`. `SweepService` written against
`Account` crashes at runtime.
**GOOD** split the contract: `ReadableAccount` and `WithdrawableAccount`. `FixedDepositAccount` implements
only `ReadableAccount`, so passing it to `SweepService` is a compile error.

### I - Interface Segregation
**BAD** `PaymentGateway` has charge/refund/settle/report/rotateKeys; `RefundOnlyGateway` stubs four methods
with `UnsupportedOperationException`.
**GOOD** `Charger`, `Refunder`, `Settler`. `RefundService implements Refunder` only; `FullGateway` implements all three.

### D - Dependency Inversion
**BAD** `TransferService` does `new MySqlLedger()`; untestable without MySQL.
**GOOD** `TransferService(Ledger ledger)`; `Ledger` is an interface owned by the high-level module.

## Exercises in this module
1. **Wallet** (`module01.exercise1`): encapsulated balance, non-negative invariant, cross-currency rejection,
   movement history, thread-safe via `synchronized`. Test proves exact totals with 10 threads.
2. **Fee strategies** (`module01.exercise2`): `FeeStrategy` with `PercentageFee`, `FlatFee`, `TieredFee`;
   `CardFeeContext` applies a 50 % discount for `CustomerTier.PREMIUM`, and the strategy can be swapped at runtime.
3. **Your turn**: add a `CryptoFeePolicy` to `solid.ocp.good` without touching `FeeCalculator`.

## Key Takeaways
- Favour composition; inheritance is for *is-a* with a stable contract.
- `final` fields + no setters + defensive copies = immutable enough for concurrency.
- Sealed types + pattern matching give you exhaustive polymorphism without `default`.
- A `default` branch that returns a "safe" value is a bug in fintech. Fail loudly.
- Inject dependencies through constructors; depend on interfaces you own.
- `BigDecimal` for money, always; set the scale explicitly.

## Cheat Sheet
| Smell | Principle | Fix |
|---|---|---|
| class with "and" in its description | SRP | split collaborators |
| `switch` on type string | OCP | strategy/policy list or sealed types |
| subclass throws `UnsupportedOperationException` | LSP | split the interface |
| implementors stub methods | ISP | role interfaces |
| `new ConcreteDependency()` inside a service | DIP | constructor injection |
| public setter on money field | Encapsulation | operations that preserve invariants |

## 10 Interview Q&A
1. **Composition vs inheritance?** Composition reuses behaviour by delegation and is changeable at runtime; inheritance couples subclass to superclass internals. Prefer composition unless there is a true is-a relationship with a stable contract.
2. **How does LSP relate to exceptions?** A subtype must not throw new checked exceptions or `UnsupportedOperationException` for operations the supertype promises. Strengthening preconditions breaks substitutability.
3. **Why is `switch` on a type string an OCP violation?** Every new type edits the switch; the class is never closed. Use a registry of policies or a sealed hierarchy.
4. **What makes a class immutable?** `final` class, `final` fields, no setters, defensive copies in/out, no leaking `this` from the constructor.
5. **Is a record immutable?** Shallowly. Its components are `final`, but a `List` component can still be mutated unless you copy it in the compact constructor.
6. **Template method vs strategy?** Template method fixes the algorithm skeleton in a superclass; strategy injects the varying part as an object. Strategy is more flexible and testable.
7. **Why sealed interfaces?** Exhaustive `switch` without `default`; the compiler fails when a new subtype is added and a case is missing.
8. **How do you unit-test a DIP-compliant service?** Inject an in-memory fake (`InMemoryLedger`). No mocking framework needed.
9. **What does ISP buy you in a microservice?** Clients compile against only the methods they use, so you can version and deploy capabilities independently.
10. **Where does validation belong?** At the boundary (constructor / factory) so that an object can never exist in an invalid state; downstream code does not re-validate.

## Exercises for the reader
1. Add `blockedMerchants(Set<String>)` to `StandardChecks` and prove with a test that it composes with the others.
2. Make `BankAccount` lock-free using `AtomicReference<BigDecimal>` and a CAS loop; keep the history consistent.
3. Convert `CardFeeContext` to accept a `Map<CustomerTier, BigDecimal>` of discounts without breaking existing tests.
