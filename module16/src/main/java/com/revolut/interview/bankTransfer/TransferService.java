package com.revolut.interview.bankTransfer;

import com.revolut.interview.bank.Account;
import com.revolut.interview.bank.AccountId;
import com.revolut.interview.bank.Bank;
import com.revolut.interview.bank.InsufficientFundsException;
import com.revolut.interview.bank.Money;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Kata 2: "Transfer money between two accounts". Requirements that interviewers check:
 * atomic (both legs or neither), no negative balance, no deadlock under A->B / B->A, idempotent by request id,
 * money conserved. Lock ordering by account id; per-account locks live here so Account stays simple.
 */
public final class TransferService {
    public enum Status { COMPLETED, INSUFFICIENT_FUNDS, DUPLICATE, REJECTED }

    public record TransferRequest(String requestId, AccountId from, AccountId to, Money amount) {
        public TransferRequest {
            Objects.requireNonNull(requestId);
            Objects.requireNonNull(from);
            Objects.requireNonNull(to);
            Objects.requireNonNull(amount);
        }
    }

    public record TransferResult(String requestId, Status status, String message) {
    }

    private final Bank bank;
    private final ConcurrentHashMap<AccountId, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TransferResult> processed = new ConcurrentHashMap<>();

    public TransferService(Bank bank) {
        this.bank = Objects.requireNonNull(bank);
    }

    public TransferResult transfer(TransferRequest req) {
        // Idempotency: claim the request id first; a duplicate returns the original outcome (or DUPLICATE while in flight).
        TransferResult placeholder = new TransferResult(req.requestId(), Status.DUPLICATE, "in flight");
        TransferResult prior = processed.putIfAbsent(req.requestId(), placeholder);
        if (prior != null) return prior.status() == Status.DUPLICATE ? prior : new TransferResult(req.requestId(), Status.DUPLICATE, "already processed: " + prior.status());

        TransferResult result = doTransfer(req);
        processed.put(req.requestId(), result);
        return result;
    }

    private TransferResult doTransfer(TransferRequest req) {
        if (!req.amount().isPositive()) return new TransferResult(req.requestId(), Status.REJECTED, "amount must be positive");
        if (req.from().equals(req.to())) return new TransferResult(req.requestId(), Status.REJECTED, "same account");
        Account from, to;
        try {
            from = bank.get(req.from());
            to = bank.get(req.to());
        } catch (IllegalArgumentException e) {
            return new TransferResult(req.requestId(), Status.REJECTED, e.getMessage());
        }
        if (!from.balance().currency().equals(req.amount().currency()) || !to.balance().currency().equals(req.amount().currency())) {
            return new TransferResult(req.requestId(), Status.REJECTED, "currency mismatch");
        }

        ReentrantLock first = lockFor(min(req.from(), req.to()));
        ReentrantLock second = lockFor(max(req.from(), req.to()));
        first.lock();
        try {
            second.lock();
            try {
                try {
                    from.withdraw(req.amount());
                } catch (InsufficientFundsException e) {
                    return new TransferResult(req.requestId(), Status.INSUFFICIENT_FUNDS, e.getMessage());
                }
                to.deposit(req.amount()); // cannot fail after validation above; if it did we would compensate
                return new TransferResult(req.requestId(), Status.COMPLETED, "ok");
            } finally {
                second.unlock();
            }
        } finally {
            first.unlock();
        }
    }

    private ReentrantLock lockFor(AccountId id) {
        return locks.computeIfAbsent(id, k -> new ReentrantLock());
    }

    private static AccountId min(AccountId a, AccountId b) {
        return a.value().compareTo(b.value()) <= 0 ? a : b;
    }

    private static AccountId max(AccountId a, AccountId b) {
        return a.value().compareTo(b.value()) <= 0 ? b : a;
    }
}
