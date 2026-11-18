package module07.lifecycle;

import java.math.BigDecimal;

/** Commands that move a transaction through its lifecycle. Sealed, so (state, command) pairs can be matched exhaustively. */
public sealed interface TxCommand {
    record Authorise(String authCode) implements TxCommand { }

    record Capture(BigDecimal amount) implements TxCommand { }

    record Settle() implements TxCommand { }

    record Refund(BigDecimal amount) implements TxCommand { }

    record Reverse() implements TxCommand { }

    record Fail(String reason) implements TxCommand { }
}
