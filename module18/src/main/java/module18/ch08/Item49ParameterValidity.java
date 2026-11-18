package module18.ch08;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Item 49 - Check parameters for validity.
 *
 * <p>Validate at the start of public methods and constructors, and fail fast with the right exception
 * ({@code NullPointerException} via {@link Objects#requireNonNull}, {@code IllegalArgumentException},
 * {@code IndexOutOfBoundsException} via {@link Objects#checkIndex} (Java 9)). A bad value detected late corrupts
 * state or fails with a misleading error far away. Records validate in the compact constructor. Document the
 * restrictions with {@code @throws}. Skip checks only if they are expensive and implicitly performed anyway.
 */
public final class Item49ParameterValidity {
    private Item49ParameterValidity() {
    }

    public record Transfer(String fromAccount, String toAccount, BigDecimal amount) {
        /**
         * @throws NullPointerException     if any argument is null
         * @throws IllegalArgumentException if amount is not positive or accounts are equal
         */
        public Transfer {
            Objects.requireNonNull(fromAccount, "fromAccount");
            Objects.requireNonNull(toAccount, "toAccount");
            Objects.requireNonNull(amount, "amount");
            if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive: " + amount);
            if (fromAccount.equals(toAccount)) throw new IllegalArgumentException("from and to must differ");
        }
    }

    /** @throws IndexOutOfBoundsException if index is outside the batch */
    public static Transfer nth(List<Transfer> batch, int index) {
        Objects.checkIndex(index, batch.size());
        return batch.get(index);
    }
}
