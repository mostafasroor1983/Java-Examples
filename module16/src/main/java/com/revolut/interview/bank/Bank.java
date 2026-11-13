package com.revolut.interview.bank;

import java.util.Currency;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Account registry: open is idempotent-safe via putIfAbsent, lookup returns Optional. */
public final class Bank {
    private final ConcurrentHashMap<AccountId, Account> accounts = new ConcurrentHashMap<>();

    public Account open(AccountId id, Currency currency) {
        Account created = new Account(id, currency);
        Account existing = accounts.putIfAbsent(id, created);
        if (existing != null) throw new IllegalStateException("account already exists: " + id.value());
        return created;
    }

    public Optional<Account> find(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public Account get(AccountId id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("unknown account: " + id.value()));
    }

    public int size() {
        return accounts.size();
    }
}
