package com.revolut.buildit.currencyexchange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Build It #5: multi-currency wallet exchange. Rates are published as immutable snapshots (AtomicReference),
 * conversions quote mid rate +/- a spread, exchange debits one pocket and credits another atomically,
 * and a rate that is too stale is rejected.
 */
public final class CurrencyExchange {
    public record RateSnapshot(Map<String, BigDecimal> toUsd, long version) {
        public RateSnapshot {
            toUsd = Map.copyOf(toUsd);
        }
    }

    public record Quote(String from, String to, BigDecimal amount, BigDecimal rate, BigDecimal result, long rateVersion) {
    }

    public enum Outcome { COMPLETED, INSUFFICIENT_FUNDS, UNKNOWN_CURRENCY, STALE_QUOTE, INVALID_AMOUNT, SAME_CURRENCY, INVALID_QUOTE }

    private final AtomicReference<RateSnapshot> rates = new AtomicReference<>(new RateSnapshot(Map.of("USD", BigDecimal.ONE), 0));
    private final BigDecimal spreadBps; // e.g. 50 = 0.5%
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, BigDecimal>> wallets = new ConcurrentHashMap<>();

    public CurrencyExchange(BigDecimal spreadBps) {
        if (spreadBps.signum() < 0) throw new IllegalArgumentException("spread must be >= 0");
        this.spreadBps = spreadBps;
    }

    /** Publish a new full snapshot; readers never see a half-updated table. */
    public long publishRates(Map<String, BigDecimal> toUsd) {
        for (BigDecimal r : toUsd.values()) if (r.signum() <= 0) throw new IllegalArgumentException("rates must be positive");
        return rates.updateAndGet(prev -> {
            var merged = new java.util.HashMap<>(toUsd);
            merged.put("USD", BigDecimal.ONE);
            return new RateSnapshot(merged, prev.version() + 1);
        }).version();
    }

    public Optional<Quote> quote(String from, String to, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || from.equals(to)) return Optional.empty();
        return priceFrom(rates.get(), from, to, amount);
    }

    /** Pure pricing from one snapshot: used to issue quotes AND to verify them at execution. */
    private Optional<Quote> priceFrom(RateSnapshot snap, String from, String to, BigDecimal amount) {
        BigDecimal fromUsd = snap.toUsd().get(from), toUsd = snap.toUsd().get(to);
        if (fromUsd == null || toUsd == null) return Optional.empty();
        BigDecimal mid = fromUsd.divide(toUsd, 10, RoundingMode.HALF_EVEN);
        BigDecimal rate = mid.multiply(BigDecimal.ONE.subtract(spreadBps.movePointLeft(4))).setScale(8, RoundingMode.HALF_EVEN);
        BigDecimal result = amount.multiply(rate).setScale(2, RoundingMode.DOWN); // never give the customer the rounding
        return Optional.of(new Quote(from, to, amount, rate, result, snap.version()));
    }

    public void deposit(String customerId, String currency, BigDecimal amount) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        wallet(customerId).merge(currency, amount, BigDecimal::add);
    }

    public BigDecimal balance(String customerId, String currency) {
        return wallet(customerId).getOrDefault(currency, BigDecimal.ZERO);
    }

    /** Execute a quote: must still be on the current rate version, else STALE_QUOTE. */
    public Outcome exchange(String customerId, Quote q) {
        Objects.requireNonNull(q);
        if (q.amount().signum() <= 0) return Outcome.INVALID_AMOUNT;
        if (q.from().equals(q.to())) return Outcome.SAME_CURRENCY;
        RateSnapshot snap = rates.get();
        if (!snap.toUsd().containsKey(q.from()) || !snap.toUsd().containsKey(q.to())) return Outcome.UNKNOWN_CURRENCY;
        if (snap.version() != q.rateVersion()) return Outcome.STALE_QUOTE;
        // Quote is a public record, so a caller can construct one with any result. Never trust client-supplied prices:
        // re-derive it from the SAME snapshot (versions match, so an honest quote reproduces exactly).
        Quote expected = priceFrom(snap, q.from(), q.to(), q.amount()).orElseThrow();
        if (expected.rate().compareTo(q.rate()) != 0 || expected.result().compareTo(q.result()) != 0) return Outcome.INVALID_QUOTE;
        ConcurrentHashMap<String, BigDecimal> w = wallet(customerId);
        synchronized (w) { // two-pocket atomicity for one customer
            BigDecimal have = w.getOrDefault(q.from(), BigDecimal.ZERO);
            if (have.compareTo(q.amount()) < 0) return Outcome.INSUFFICIENT_FUNDS;
            w.put(q.from(), have.subtract(q.amount()));
            w.merge(q.to(), q.result(), BigDecimal::add);
            return Outcome.COMPLETED;
        }
    }

    public long currentRateVersion() {
        return rates.get().version();
    }

    private ConcurrentHashMap<String, BigDecimal> wallet(String customerId) {
        return wallets.computeIfAbsent(customerId, k -> new ConcurrentHashMap<>());
    }
}
