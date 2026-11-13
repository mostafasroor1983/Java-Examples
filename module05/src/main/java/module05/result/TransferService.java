package module05.result;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Shows Result in a pipeline: validate -> debit -> credit, each step typed. */
public final class TransferService {
    public enum TransferError { INVALID_AMOUNT, UNKNOWN_ACCOUNT, INSUFFICIENT_FUNDS, SAME_ACCOUNT }

    public record Receipt(String from, String to, BigDecimal amount) {
    }

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();

    public void open(String account, BigDecimal balance) {
        balances.put(account, balance);
    }

    public BigDecimal balance(String account) {
        return balances.getOrDefault(account, BigDecimal.ZERO);
    }

    public Result<Receipt, TransferError> transfer(String from, String to, BigDecimal amount) {
        return validate(from, to, amount)
                .flatMap(a -> debit(from, a))
                .map(a -> {
                    balances.merge(to, a, BigDecimal::add);
                    return new Receipt(from, to, a);
                });
    }

    private Result<BigDecimal, TransferError> validate(String from, String to, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return Result.err(TransferError.INVALID_AMOUNT);
        if (from.equals(to)) return Result.err(TransferError.SAME_ACCOUNT);
        if (!balances.containsKey(from) || !balances.containsKey(to)) return Result.err(TransferError.UNKNOWN_ACCOUNT);
        return Result.ok(amount);
    }

    private synchronized Result<BigDecimal, TransferError> debit(String from, BigDecimal amount) {
        BigDecimal bal = balances.get(from);
        if (bal.compareTo(amount) < 0) return Result.err(TransferError.INSUFFICIENT_FUNDS);
        balances.put(from, bal.subtract(amount));
        return Result.ok(amount);
    }
}
