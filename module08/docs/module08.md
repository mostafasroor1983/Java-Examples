# Module 08 - Exceptions and Error Handling

Package root: `module08`. Domain: payment failures, provider outages, ledger resources, API error bodies.

## The hierarchy
```
Throwable
 +- Error                         -> LedgerCorruptedError (never catch; fail fast)
 +- Exception (checked)           -> IOException, TimeoutException (caller must handle)
     +- RuntimeException          -> PaymentException (domain root, unchecked, has code() + retryable())
         +- InsufficientFundsException   402, not retryable
         +- InvalidPaymentException      400, not retryable
         +- PaymentProviderException     502/503, retryable flag, wraps the technical cause
```

## BAD vs GOOD
```java
// BAD: swallow
try { return client.authorise(id); } catch (Exception e) { return null; }
// BAD: lose the cause
catch (Exception e) { throw new PaymentProviderException("failed: " + e.getMessage(), null, false); }
// GOOD: translate at the boundary, classify, keep the cause, restore interrupts
catch (SocketTimeoutException | TimeoutException e) { throw new PaymentProviderException("timeout", e, true); }
catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new PaymentProviderException("interrupted", e, true); }
catch (RuntimeException e) { throw e; } // do not double-wrap
```
```java
// BAD: leaks on failure
LedgerConnection c = new LedgerConnection(...); c.write(entry); c.close();
// GOOD
try (LedgerConnection c = new LedgerConnection(...)) { c.write(entry); }
```

## try-with-resources facts proven by tests
- Resources close in **reverse** declaration order (`close replica` before `close primary`).
- If body and `close()` both throw, the body exception propagates and `close()`'s is attached via `getSuppressed()`.
- Java 9+: an effectively final variable can be used directly: `try (existing) { ... }`.
- `finally` is still right for non-resource cleanup (timers, MDC, metrics).

## Circuit breaker (`module08.circuitbreaker.CircuitBreaker`)
```
CLOSED --(N consecutive failures)--> OPEN --(cooldown elapsed)--> HALF_OPEN --success--> CLOSED
                                                                   \--failure--> OPEN (cooldown restarts)
```
- Fail fast while OPEN: `CircuitOpenException` without calling the provider (16 concurrent callers, 0 executed).
- Exactly one trial call in HALF_OPEN (CAS on `trialInFlight`).
- Clock injected as `Supplier<Instant>` so tests are deterministic, no sleeps.
- `RetryPolicy` retries only `retryable()` exceptions and attaches earlier attempts as suppressed.

## API mapping (`PaymentErrorResponse`)
Pattern-matching switch maps exception type -> HTTP status, stable `code`, safe `message`, `retryable`, details.
Unknown exceptions become `500 INTERNAL_ERROR / "Unexpected error"`: internals never leak.

## Key Takeaways
- Checked for recoverable conditions the caller must decide about; unchecked for bugs and business-rule violations; `Error` for broken invariants.
- Always chain the cause. Never `catch (Exception e) {}`. Never `catch (Throwable)` in request paths.
- Restore the interrupt flag when you catch `InterruptedException` and cannot rethrow it.
- Use `try-with-resources` for anything `AutoCloseable`; it handles ordering and suppression correctly.
- Design exceptions with stable codes and a `retryable` flag; clients and retry policies rely on them.
- Breakers + bounded retries + timeouts are the trio that keeps a payments service alive when a dependency dies.

## Cheat Sheet
| Situation | Do |
|---|---|
| Expected business outcome | return a `Result`/sealed type (module05/07) or throw a domain unchecked exception with a code |
| Infrastructure failure | wrap in domain exception, keep cause, mark retryable |
| Caught `InterruptedException` | `Thread.currentThread().interrupt()` then rethrow/wrap |
| Resource cleanup | try-with-resources |
| Both body and close fail | body wins, close is suppressed |
| Dependency flapping | circuit breaker + bounded retry + timeout |
| HTTP mapping | one place, pattern switch, no internal details |

## 10 Interview Q&A
1. **Checked vs unchecked - when?** Checked when the immediate caller can realistically recover (missing file, retryable I/O); unchecked for programming errors and business rule violations that propagate to a boundary.
2. **Why is swallowing exceptions dangerous in payments?** You lose the signal that money may be in an unknown state; silent `null` returns create reconciliation nightmares.
3. **What is exception chaining?** Passing the original exception as `cause` so the full stack trace survives translation between layers.
4. **What happens if `finally` throws?** It replaces the exception from the `try` block; the original is lost. try-with-resources avoids this via suppression.
5. **Order of `close()` in try-with-resources?** Reverse of declaration.
6. **What is a suppressed exception?** An exception attached to another via `addSuppressed`, used when cleanup fails after a primary failure.
7. **Why restore the interrupt flag?** Thread pools and higher-level code use it to stop work; swallowing it can make shutdown hang.
8. **Circuit breaker states?** CLOSED (normal), OPEN (fail fast), HALF_OPEN (probe). Transition on failure threshold and cooldown.
9. **Retry everything?** No. Retry only idempotent operations with retryable failures, with bounded attempts and backoff; never retry 4xx-class errors.
10. **Cost of exceptions?** Creating one captures the stack trace (expensive); throwing/catching in hot paths is slow. Do not use exceptions for control flow.

## Exercises for the reader
1. Add exponential backoff with jitter to `RetryPolicy` using an injected `Sleeper` interface so tests stay fast.
2. Make `CircuitBreaker` count failures in a sliding window (rate-based) instead of consecutive failures.
3. Add a `traceId` to `PaymentErrorResponse` and prove it is present for every mapped exception.
