package com.revolut.interview.bank;

public final class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(AccountId id, Money balance, Money requested) {
        super("account " + id.value() + " has " + balance.amount() + ", requested " + requested.amount());
    }
}
