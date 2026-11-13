package module05.wildcards;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * PECS: Producer Extends, Consumer Super.
 * - `? extends T`: you READ T's out (producer). You cannot add (except null).
 * - `? super T`:   you WRITE T's in (consumer). You read only Object.
 * - `?`:           unknown; read Object, add nothing.
 */
public final class Wildcards {
    private Wildcards() {
    }

    public interface Transaction {
        BigDecimal amount();
    }

    public record CardTx(BigDecimal amount) implements Transaction {
    }

    public record BankTx(BigDecimal amount) implements Transaction {
    }

    /** Producer: accepts List<CardTx>, List<BankTx>, List<Transaction>. */
    public static BigDecimal total(Collection<? extends Transaction> txs) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction t : txs) sum = sum.add(t.amount());
        return sum;
    }

    /** Consumer: can sink CardTx into List<CardTx>, List<Transaction>, List<Object>. */
    public static void addCardTxs(Collection<? super CardTx> sink, int n) {
        for (int i = 0; i < n; i++) sink.add(new CardTx(BigDecimal.ONE));
    }

    /** Both: copy from a producer into a consumer (same signature as Collections.copy). */
    public static <T> void copy(List<? super T> dest, List<? extends T> src) {
        for (T t : src) dest.add(t);
    }

    /** Unbounded wildcard: only Object-level operations. */
    public static int countNonNull(Collection<?> items) {
        int n = 0;
        for (Object o : items) if (o != null) n++;
        return n;
    }

    /** Invariance: List<CardTx> is NOT a List<Transaction>. This helper shows the safe way to widen. */
    public static List<Transaction> widen(List<? extends Transaction> specific) {
        return new ArrayList<>(specific);
    }
}
