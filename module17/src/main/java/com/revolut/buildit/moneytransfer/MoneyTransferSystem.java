package com.revolut.buildit.moneytransfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Build It #3: money transfer with accounts, idempotent transfers, per-account history, daily outbound limit,
 * and an audit trail. Lock ordering by account id; typed outcomes; injected clock for limits.
 */
public final class MoneyTransferSystem {
    public enum Outcome { COMPLETED, INSUFFICIENT_FUNDS, DAILY_LIMIT_EXCEEDED, UNKNOWN_ACCOUNT, SAME_ACCOUNT, INVALID_AMOUNT, DUPLICATE }

    public record Transfer(String id, String from, String to, BigDecimal amount, Outcome outcome, Instant at) {
    }

    private static final class Account {
        final ReentrantLock lock = new ReentrantLock();
        BigDecimal balance;
        final List<Transfer> history = new ArrayList<>();
        BigDecimal sentToday = BigDecimal.ZERO;
        java.time.LocalDate today;

        Account(BigDecimal balance) {
            this.balance = balance;
        }
    }

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, Transfer> byId = new ConcurrentHashMap<>();
    private final BigDecimal dailyLimit;
    private final Supplier<Instant> clock;

    public MoneyTransferSystem(BigDecimal dailyLimit, Supplier<Instant> clock) {
        this.dailyLimit = Objects.requireNonNull(dailyLimit);
        this.clock = Objects.requireNonNull(clock);
    }

    public void openAccount(String id, BigDecimal opening) {
        if (opening.signum() < 0) throw new IllegalArgumentException("opening must be >= 0");
        if (accounts.putIfAbsent(id, new Account(opening)) != null) throw new IllegalStateException("exists: " + id);
    }

    public Transfer transfer(String transferId, String from, String to, BigDecimal amount) {
        Instant now = clock.get();
        Transfer placeholder = new Transfer(transferId, from, to, amount, Outcome.DUPLICATE, now);
        Transfer prior = byId.putIfAbsent(transferId, placeholder);
        if (prior != null) return prior.outcome() == Outcome.DUPLICATE ? prior : new Transfer(transferId, from, to, amount, Outcome.DUPLICATE, now);

        Transfer result;
        try {
            result = execute(transferId, from, to, amount, now);
        } catch (RuntimeException e) {
            byId.remove(transferId, placeholder); // unexpected failure: release the id so the client can retry
            throw e;
        }
        byId.put(transferId, result);
        return result;
    }

    private Transfer execute(String id, String from, String to, BigDecimal amount, Instant now) {
        if (amount == null || amount.signum() <= 0) return new Transfer(id, from, to, amount, Outcome.INVALID_AMOUNT, now);
        if (from.equals(to)) return new Transfer(id, from, to, amount, Outcome.SAME_ACCOUNT, now);
        Account a = accounts.get(from), b = accounts.get(to);
        if (a == null || b == null) return new Transfer(id, from, to, amount, Outcome.UNKNOWN_ACCOUNT, now);

        Account first = from.compareTo(to) < 0 ? a : b, second = first == a ? b : a;
        first.lock.lock();
        try {
            second.lock.lock();
            try {
                rollDay(a, now);
                Outcome outcome;
                if (a.balance.compareTo(amount) < 0) outcome = Outcome.INSUFFICIENT_FUNDS;
                else if (a.sentToday.add(amount).compareTo(dailyLimit) > 0) outcome = Outcome.DAILY_LIMIT_EXCEEDED;
                else {
                    a.balance = a.balance.subtract(amount);
                    b.balance = b.balance.add(amount);
                    a.sentToday = a.sentToday.add(amount);
                    outcome = Outcome.COMPLETED;
                }
                Transfer t = new Transfer(id, from, to, amount, outcome, now);
                a.history.add(t);
                if (outcome == Outcome.COMPLETED) b.history.add(t);
                return t;
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }

    private void rollDay(Account a, Instant now) {
        java.time.LocalDate day = now.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        // Only roll FORWARD. The timestamp is read before the lock is acquired, so a thread stamped 23:59:59 can run
        // after one stamped 00:00:01; rolling back would wipe today's spent amount and bypass the daily limit.
        if (a.today == null || day.isAfter(a.today)) {
            a.today = day;
            a.sentToday = BigDecimal.ZERO;
        }
    }

    public BigDecimal balance(String id) {
        Account a = require(id);
        a.lock.lock();
        try {
            return a.balance;
        } finally {
            a.lock.unlock();
        }
    }

    public List<Transfer> history(String id) {
        Account a = require(id);
        a.lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(a.history));
        } finally {
            a.lock.unlock();
        }
    }

    public Optional<Transfer> find(String transferId) {
        return Optional.ofNullable(byId.get(transferId));
    }

    public BigDecimal totalMoney() {
        BigDecimal t = BigDecimal.ZERO;
        for (String id : accounts.keySet()) t = t.add(balance(id));
        return t;
    }

    private Account require(String id) {
        Account a = accounts.get(id);
        if (a == null) throw new IllegalArgumentException("unknown account " + id);
        return a;
    }
}
