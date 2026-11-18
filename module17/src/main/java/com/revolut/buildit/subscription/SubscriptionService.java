package com.revolut.buildit.subscription;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Build It #4: subscriptions with plans (STANDARD/PLUS/PREMIUM/METAL), monthly billing, upgrade/downgrade with
 * daily proration, cancel at period end, and a billing run that charges everything due on a date.
 */
public final class SubscriptionService {
    public enum Plan {
        STANDARD("0.00"), PLUS("3.99"), PREMIUM("7.99"), METAL("14.99");
        final BigDecimal monthlyPrice;

        Plan(String p) {
            this.monthlyPrice = new BigDecimal(p);
        }

        public BigDecimal monthlyPrice() {
            return monthlyPrice;
        }
    }

    public enum Status { ACTIVE, PENDING_CANCELLATION, CANCELLED }

    public record Subscription(String customerId, Plan plan, Status status, LocalDate periodStart, LocalDate nextBillingDate) {
        Subscription with(Plan p, Status s, LocalDate start, LocalDate next) {
            return new Subscription(customerId, p, s, start, next);
        }
    }

    public record Charge(String customerId, BigDecimal amount, String reason, LocalDate on) {
    }

    private final Map<String, Subscription> subs = new ConcurrentHashMap<>();
    private final List<Charge> charges = new ArrayList<>();
    private final Supplier<LocalDate> today;

    public SubscriptionService(Supplier<LocalDate> today) {
        this.today = Objects.requireNonNull(today);
    }

    public Subscription subscribe(String customerId, Plan plan) {
        LocalDate now = today.get();
        Subscription created = new Subscription(customerId, plan, Status.ACTIVE, now, now.plusMonths(1));
        if (subs.putIfAbsent(customerId, created) != null) throw new IllegalStateException("already subscribed: " + customerId);
        charge(customerId, plan.monthlyPrice(), "initial " + plan, now);
        return created;
    }

    /** Immediate plan change; charge or credit the prorated difference for the remaining days of the period. */
    public Subscription changePlan(String customerId, Plan newPlan) {
        return subs.compute(customerId, (id, s) -> {
            if (s == null) throw new IllegalArgumentException("not subscribed: " + id);
            if (s.status() == Status.CANCELLED) throw new IllegalStateException("subscription cancelled");
            if (s.plan() == newPlan) return s;
            LocalDate now = today.get();
            long periodDays = ChronoUnit.DAYS.between(s.periodStart(), s.nextBillingDate());
            long remaining = Math.max(0, ChronoUnit.DAYS.between(now, s.nextBillingDate()));
            BigDecimal diff = newPlan.monthlyPrice().subtract(s.plan().monthlyPrice())
                    .multiply(BigDecimal.valueOf(remaining))
                    .divide(BigDecimal.valueOf(periodDays), 2, RoundingMode.HALF_EVEN);
            charge(id, diff, "prorated " + s.plan() + " -> " + newPlan, now);
            return s.with(newPlan, s.status(), s.periodStart(), s.nextBillingDate());
        });
    }

    /** Cancel at period end: stays active until nextBillingDate, then no renewal. */
    public Subscription cancel(String customerId) {
        return subs.compute(customerId, (id, s) -> {
            if (s == null) throw new IllegalArgumentException("not subscribed: " + id);
            if (s.status() == Status.CANCELLED) return s;
            return s.with(s.plan(), Status.PENDING_CANCELLATION, s.periodStart(), s.nextBillingDate());
        });
    }

    /** Billing run: renew everything due on or before today; finalise pending cancellations. Returns number of renewals. */
    public int runBilling() {
        LocalDate now = today.get();
        int renewed = 0;
        for (String id : List.copyOf(subs.keySet())) {
            boolean[] charged = {false};
            subs.computeIfPresent(id, (k, s) -> {
                // Every condition is re-checked INSIDE the atomic compute: a check outside it lets two overlapping
                // billing runs both see "due" and charge twice.
                if (s.status() == Status.CANCELLED || s.nextBillingDate().isAfter(now)) return s; // never bill cancelled
                if (s.status() == Status.PENDING_CANCELLATION) return s.with(s.plan(), Status.CANCELLED, s.periodStart(), s.nextBillingDate());
                charged[0] = true;
                charge(k, s.plan().monthlyPrice(), "renewal " + s.plan(), now);
                return s.with(s.plan(), Status.ACTIVE, s.nextBillingDate(), s.nextBillingDate().plusMonths(1));
            });
            if (charged[0]) renewed++;
        }
        return renewed;
    }

    public Optional<Subscription> find(String customerId) {
        return Optional.ofNullable(subs.get(customerId));
    }

    public synchronized List<Charge> charges(String customerId) {
        return charges.stream().filter(c -> c.customerId().equals(customerId)).toList();
    }

    private synchronized void charge(String customerId, BigDecimal amount, String reason, LocalDate on) {
        if (amount.signum() == 0) return; // STANDARD is free, nothing to record
        charges.add(new Charge(customerId, amount.setScale(2, RoundingMode.HALF_EVEN), reason, on));
    }
}
