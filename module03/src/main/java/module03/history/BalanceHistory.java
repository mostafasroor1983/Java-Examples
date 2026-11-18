package module03.history;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Daily closing balances keyed by value date in a {@link TreeMap}, answering "what was the balance on date X?"
 * for interest accrual, statements and regulatory reporting.
 *
 * <p>{@code floorEntry(date)} returns the last closing balance on or before the date in O(log n): days without
 * activity simply inherit the previous balance, so we never store a row per calendar day.
 *
 * <p>Back-valued postings (a correction booked today with an earlier value date) shift every later closing
 * balance; {@code tailMap(valueDate, true)} finds exactly the affected range. Not thread-safe: in production the
 * source of truth is the ledger table, and this is a read model rebuilt from it.
 */
public final class BalanceHistory {
    private final NavigableMap<LocalDate, BigDecimal> closingBalances = new TreeMap<>();

    public void post(LocalDate valueDate, BigDecimal amount) {
        Objects.requireNonNull(valueDate, "valueDate");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() == 0) throw new IllegalArgumentException("zero posting");
        // create the day's row from the previous closing balance if this is the first posting that day
        closingBalances.computeIfAbsent(valueDate, d -> balanceAsOf(d.minusDays(1)));
        // apply the posting to that day AND every later day (back-valuation)
        for (Map.Entry<LocalDate, BigDecimal> e : closingBalances.tailMap(valueDate, true).entrySet()) {
            e.setValue(e.getValue().add(amount));
        }
    }

    public BigDecimal balanceAsOf(LocalDate date) {
        Map.Entry<LocalDate, BigDecimal> e = closingBalances.floorEntry(date);
        return e == null ? BigDecimal.ZERO : e.getValue();
    }

    /** Lowest end-of-day balance in [from, to], e.g. to check a minimum-balance fee waiver. */
    public BigDecimal minimumBalance(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new IllegalArgumentException("from after to");
        BigDecimal min = balanceAsOf(from); // the balance carried into the period counts
        for (BigDecimal b : closingBalances.subMap(from, true, to, true).values()) {
            if (b.compareTo(min) < 0) min = b;
        }
        return min;
    }

    /** Sum of daily balances over [from, to] inclusive: the "balance-days" figure used for average-balance interest. */
    public BigDecimal balanceDays(LocalDate from, LocalDate to) {
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) total = total.add(balanceAsOf(d));
        return total;
    }

    public int storedDays() {
        return closingBalances.size();
    }
}
