package module01.hexagonal;

import module01.immutability.Money;

import java.util.Objects;

/** Inbound command. The idempotency key is supplied by the client so retries over the network are safe. */
public record TransferCommand(String idempotencyKey, String fromAccount, String toAccount, Money amount) {
    public TransferCommand {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(fromAccount, "fromAccount");
        Objects.requireNonNull(toAccount, "toAccount");
        Objects.requireNonNull(amount, "amount");
        if (idempotencyKey.isBlank()) throw new IllegalArgumentException("idempotency key required");
        if (fromAccount.equals(toAccount)) throw new IllegalArgumentException("from and to must differ");
        if (amount.isNegative() || amount.amount().signum() == 0) throw new IllegalArgumentException("amount must be positive");
    }
}
