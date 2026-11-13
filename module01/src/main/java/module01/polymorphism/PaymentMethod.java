package module01.polymorphism;

import java.math.BigDecimal;

/** Sealed hierarchy: the compiler knows every subtype, enabling exhaustive switches. */
public sealed interface PaymentMethod permits Card, BankAccount, Wallet {
    BigDecimal fee(BigDecimal amount);

    String describe();
}
