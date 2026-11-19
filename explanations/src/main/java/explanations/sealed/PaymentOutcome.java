package explanations.sealed;

/**
 * A sealed hierarchy with exactly three permitted shapes. The compiler knows the full set, so a switch
 * without `default` is checked for exhaustiveness: add a fourth outcome and every switch stops compiling
 * until it is handled. That is the whole point - the type system carries the business rule.
 */
public sealed interface PaymentOutcome permits Success, Failure, Pending {
    String paymentId();
}
