package module02.allocation;

import module02.valueobjects.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Splits a {@link Money} amount by ratios without creating or losing a single minor unit.
 *
 * <p>Naive approach: {@code 100.00 / 3 = 33.33} x 3 = 99.99, so one cent disappears from the ledger and
 * reconciliation fails. Here the amount is converted to minor units (cents), each share gets
 * {@code total * ratio / sum} truncated toward zero, and the leftover cents go one each to the shares with the largest
 * remainders (ties broken by position, so results are deterministic and reproducible for audit).
 *
 * <p>Typical uses: splitting a bill between users, allocating interest across sub-accounts, pro-rata fee sharing.
 */
public final class MoneyAllocator {
    private MoneyAllocator() {
    }

    public static List<Money> allocate(Money total, List<Integer> ratios) {
        Objects.requireNonNull(total, "total");
        if (ratios == null || ratios.isEmpty()) throw new IllegalArgumentException("ratios required");
        if (ratios.stream().anyMatch(r -> r == null || r < 0)) throw new IllegalArgumentException("ratios must be >= 0");
        long ratioSum = ratios.stream().mapToLong(Integer::longValue).sum();
        if (ratioSum == 0) throw new IllegalArgumentException("at least one ratio must be positive");

        int scale = total.currency().getDefaultFractionDigits();
        BigDecimal minorTotal = total.amount().movePointRight(scale); // exact: Money has normalised scale
        BigDecimal sum = BigDecimal.valueOf(ratioSum);

        BigDecimal[] shares = new BigDecimal[ratios.size()];
        BigDecimal[] remainders = new BigDecimal[ratios.size()];
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < ratios.size(); i++) {
            BigDecimal exact = minorTotal.multiply(BigDecimal.valueOf(ratios.get(i)));
            BigDecimal[] qr = exact.divideAndRemainder(sum);
            // divideAndRemainder truncates toward zero: for negative totals (refund splits) the leftover is negative
            // and is handed out as -1 minor units below, so the sum is still exact
            shares[i] = qr[0];
            remainders[i] = qr[1];
            allocated = allocated.add(qr[0]);
        }
        int leftover = minorTotal.subtract(allocated).intValueExact();
        int step = Integer.signum(leftover);
        for (int n = 0; n < Math.abs(leftover); n++) {
            int best = -1;
            for (int i = 0; i < shares.length; i++) {
                if (ratios.get(i) == 0) continue; // a zero-ratio party never receives a penny
                if (best == -1 || remainders[i].abs().compareTo(remainders[best].abs()) > 0) best = i;
            }
            shares[best] = shares[best].add(BigDecimal.valueOf(step));
            remainders[best] = BigDecimal.ZERO; // each share receives at most one extra unit
        }

        List<Money> out = new ArrayList<>(shares.length);
        for (BigDecimal s : shares) {
            out.add(Money.of(s.movePointLeft(scale).setScale(scale, RoundingMode.UNNECESSARY), total.currency()));
        }
        return List.copyOf(out);
    }

    public static List<Money> splitEvenly(Money total, int parts) {
        if (parts <= 0) throw new IllegalArgumentException("parts must be > 0");
        return allocate(total, java.util.Collections.nCopies(parts, 1));
    }
}
