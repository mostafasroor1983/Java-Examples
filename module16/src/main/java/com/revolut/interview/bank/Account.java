package com.revolut.interview.bank;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Kata 1: "Implement a bank account". Invariants: balance never negative, every change recorded,
 * single currency, thread-safe. Lock is private so callers cannot interfere.
 */
public final class Account {
    public enum Kind { DEPOSIT, WITHDRAWAL }

    public record Transaction(Kind kind, Money amount, Money balanceAfter) {
    }

    private final AccountId id;
    private final Object lock = new Object();
    private Money balance;
    private final List<Transaction> transactions = new ArrayList<>();

    public Account(AccountId id, java.util.Currency currency) {
        this.id = Objects.requireNonNull(id);
        this.balance = Money.zero(currency);
    }

    public AccountId id() {
        return id;
    }

    public Money balance() {
        synchronized (lock) {
            return balance;
        }
    }

    public void deposit(Money amount) {
        requirePositive(amount);
        synchronized (lock) {
            balance = balance.plus(amount);
            transactions.add(new Transaction(Kind.DEPOSIT, amount, balance));
        }
    }

    public void withdraw(Money amount) {
        requirePositive(amount);
        synchronized (lock) {
            if (balance.compareTo(amount) < 0) throw new InsufficientFundsException(id, balance, amount);
            balance = balance.minus(amount);
            transactions.add(new Transaction(Kind.WITHDRAWAL, amount, balance));
        }
    }

    public List<Transaction> transactions() {
        synchronized (lock) {
            return List.copyOf(transactions);
        }
    }

    private static void requirePositive(Money amount) {
        Objects.requireNonNull(amount, "amount");
        if (!amount.isPositive()) throw new IllegalArgumentException("amount must be positive");
    }
}
