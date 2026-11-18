# Chapter 10 - Exceptions (items 69-77)

Package `module18.ch10`, tests in `Chapter10Test`. Theme: exceptions as part of the API contract. In payments, error
handling decides whether money moves once, twice or not at all (see also module08).

| Situation | Mechanism | Item |
|---|---|---|
| Expected outcome (declined, insufficient funds) | state-testing method / Optional / result type | 69, 71 |
| Recoverable, caller should act | checked exception with accessors | 70 |
| Caller bug | IllegalArgumentException / IllegalStateException / NPE | 70, 72 |
| Lower-layer failure | translate + chain cause + classify | 73 |
| Any failure | detailed message without secrets, state unchanged | 75, 76 |

---

## Item 69 - Use exceptions only for exceptional conditions
**In short.** Do not use exceptions for ordinary control flow. Offer a state-testing method or a distinguished return value.
**Example.** `sumBad` terminates its loop with `IndexOutOfBoundsException`. `Wallet.canDebit` and `tryDebit` (Optional) handle the
expected "not enough money" case.
**Why it matters.** Exception-driven logic is slow, obscures intent, and swallows real bugs of the same exception type.
**Proof.** `item69_stateTestingInsteadOfExceptions`.

## Item 70 - Use checked exceptions for recoverable conditions and runtime exceptions for programming errors
**In short.** Checked exceptions are for conditions the caller can reasonably recover from; give them accessors with the recovery
information. Runtime exceptions are for precondition violations. Do not subclass `Error`.
**Example.** `CardDeclinedException` (checked) exposes `declineCode()` and `retryable()`; a non-positive amount throws
`IllegalArgumentException`. Java 21 switch expression maps issuer response codes.
**Proof.** `item70_checkedCarriesRecoveryInfo`.

## Item 71 - Avoid unnecessary use of checked exceptions
**In short.** A checked exception the caller cannot handle meaningfully adds noise and does not work with lambdas/streams. Return
an Optional or a result type instead.
**Example.** `findOrThrow` (checked `AccountNotFoundException`) vs `find` returning Optional, which composes in a stream via
`flatMap(Optional::stream)`.
**Java 17/21.** Sealed result types (`Ok`/`Err`, module05) plus pattern matching are a modern alternative.
**Proof.** `item71_optionalComposesWithStreams`.

## Item 72 - Favor the use of standard exceptions
**In short.** Reuse IAE, ISE, NPE, IOOBE, UnsupportedOperationException and ConcurrentModificationException so readers understand
immediately. Never throw `Exception`, `RuntimeException`, `Throwable` or `Error` directly.
**Example.** Negative amount -> IAE; capture after cancel -> ISE; refund of a cash payment -> UOE; installment index -> IOOBE.
**Proof.** `item72_standardExceptions`.

## Item 73 - Throw exceptions appropriate to the abstraction
**In short.** Translate low-level exceptions at layer boundaries into exceptions that make sense at that level, and chain the
cause. Classify while translating.
**Example.** `SQLTransientConnectionException` becomes `LedgerUnavailableException(retryable = true)`; other `SQLException`s become
the same type with `retryable = false`; the cause is preserved.
**Why it matters.** Callers decide whether to retry a payment from the classification; engineers debug from the chained cause.
**Proof.** `item73_translatedWithCauseAndClassification`.

## Item 74 - Document all exceptions thrown by each method
**In short.** Declare checked exceptions individually and document every exception (checked and unchecked) with `@throws` and its
condition; keep unchecked ones out of the `throws` clause.
**Example.** `authoriseAtm` documents `DailyLimitExceededException` and `IllegalArgumentException`; NPE is documented once at class level.
**Proof.** `item74_documentedExceptionsBehaveAsDocumented`.

## Item 75 - Include failure-capture information in detail messages
**In short.** Messages should contain the values that caused the failure, and the exception should expose them via accessors.
**Fintech caveat.** Never include secrets or more personal data than needed (no PINs, full PANs, tokens, passwords), because
messages go to logs and APM tools (see module08 `LogSanitizer`).
**Example.** `LimitExceededException("ACC-7", 1000, 250.00)` with `toPlainString()` amounts and accessors.
**Proof.** `item75_failureCaptureWithoutSecrets`.

## Item 76 - Strive for failure atomicity
**In short.** A failed call should leave the object unchanged. Validate first, order operations so failures happen before
mutation, or work on a copy and swap it in.
**Example.** `BrokenBatch` keeps half a batch after a failure; `AtomicBatch` validates everything, builds a copy, and swaps once.
**Why it matters.** Half-posted batches unbalance the ledger.
**Proof.** `item76_failureAtomicity`.

## Item 77 - Don't ignore exceptions
**In short.** An empty catch block hides failures. If ignoring is right, name the variable `ignored`, explain why, and ideally count it.
**Example.** `notifyBad` swallows an SMS gateway failure and reports success. `notifyCustomer` returns false so the caller retries.
`closeQuietly` ignores close failures on purpose, with a comment and a counter.
**Proof.** `item77_swallowedVsHandled`.

---
**Say it in an interview.** "Expected outcomes are results, not exceptions; checked exceptions only when the caller can recover,
with accessors; standard exceptions for bugs; translate and classify at boundaries keeping the cause; detailed but secret-free
messages; failure atomicity; never an empty catch."
