package module07.lifecycle;

import module07.lifecycle.TxCommand.Authorise;
import module07.lifecycle.TxCommand.Capture;
import module07.lifecycle.TxCommand.Fail;
import module07.lifecycle.TxCommand.Refund;
import module07.lifecycle.TxCommand.Reverse;
import module07.lifecycle.TxCommand.Settle;
import module07.lifecycle.TxState.Authorised;
import module07.lifecycle.TxState.Captured;
import module07.lifecycle.TxState.Failed;
import module07.lifecycle.TxState.Initiated;
import module07.lifecycle.TxState.Reversed;
import module07.lifecycle.TxState.Settled;

import java.math.BigDecimal;

/**
 * Pure transition function {@code (state, command) -> new state}. No mutation, no I/O, so it is trivially
 * testable, replayable from an event log, and safe to call from any thread.
 *
 * <p>Two equivalent implementations:
 * <ul>
 *   <li>{@link #apply} uses <b>Java 21</b> pattern matching for switch with record patterns and guards
 *   (final in 21; preview in Java 19 and 20, where it needs {@code --enable-preview}).</li>
 *   <li>{@link #applyJava17} uses only <b>Java 17</b> features: sealed types, records and {@code instanceof} patterns.</li>
 * </ul>
 * Tests assert that both produce identical results for every (state, command) combination.
 */
public final class TransactionLifecycle {
    private TransactionLifecycle() {
    }

    /** Java 21: record patterns deconstruct state and command in one place; guards express business limits. */
    public static TxState apply(TxState state, TxCommand cmd) {
        record Transition(TxState state, TxCommand cmd) { }
        return switch (new Transition(state, cmd)) {
            case Transition(Initiated(var id, var amt), Authorise(var code)) -> new Authorised(id, amt, code);
            case Transition(Initiated(var id, var amt), Fail(var reason)) -> new Failed(id, reason);
            case Transition(Authorised(var id, var auth, var code), Capture(var amt))
                    when amt.signum() > 0 && amt.compareTo(auth) <= 0 -> new Captured(id, auth, amt);
            case Transition(Authorised(var id, var auth, var code), Capture(var amt)) ->
                    throw new IllegalArgumentException("capture " + amt + " outside (0, " + auth + "] for " + id);
            case Transition(Authorised(var id, var auth, var code), Reverse r) -> new Reversed(id);
            case Transition(Authorised(var id, var auth, var code), Fail(var reason)) -> new Failed(id, reason);
            case Transition(Captured(var id, var auth, var captured), Settle s) -> new Settled(id, captured, BigDecimal.ZERO);
            case Transition(Settled s, Refund(var amt))
                    when amt.signum() > 0 && amt.compareTo(s.refundable()) <= 0 ->
                    new Settled(s.txId(), s.settled(), s.refunded().add(amt));
            case Transition(Settled s, Refund(var amt)) ->
                    throw new IllegalArgumentException("refund " + amt + " exceeds refundable " + s.refundable());
            case Transition t -> throw new IllegalStateException(
                    t.cmd().getClass().getSimpleName() + " not allowed in " + t.state().getClass().getSimpleName()
                            + " for " + t.state().txId());
        };
    }

    /** Java 17 equivalent: same rules, expressed with instanceof patterns. More verbose, no compile-time exhaustiveness. */
    public static TxState applyJava17(TxState state, TxCommand cmd) {
        if (state instanceof Initiated i) {
            if (cmd instanceof Authorise a) return new Authorised(i.txId(), i.amount(), a.authCode());
            if (cmd instanceof Fail f) return new Failed(i.txId(), f.reason());
        } else if (state instanceof Authorised a) {
            if (cmd instanceof Capture c) {
                if (c.amount().signum() > 0 && c.amount().compareTo(a.authorised()) <= 0) {
                    return new Captured(a.txId(), a.authorised(), c.amount());
                }
                throw new IllegalArgumentException("capture " + c.amount() + " outside (0, " + a.authorised() + "] for " + a.txId());
            }
            if (cmd instanceof Reverse) return new Reversed(a.txId());
            if (cmd instanceof Fail f) return new Failed(a.txId(), f.reason());
        } else if (state instanceof Captured c && cmd instanceof Settle) {
            return new Settled(c.txId(), c.captured(), BigDecimal.ZERO);
        } else if (state instanceof Settled s && cmd instanceof Refund r) {
            if (r.amount().signum() > 0 && r.amount().compareTo(s.refundable()) <= 0) {
                return new Settled(s.txId(), s.settled(), s.refunded().add(r.amount()));
            }
            throw new IllegalArgumentException("refund " + r.amount() + " exceeds refundable " + s.refundable());
        }
        throw new IllegalStateException(cmd.getClass().getSimpleName() + " not allowed in "
                + state.getClass().getSimpleName() + " for " + state.txId());
    }
}
