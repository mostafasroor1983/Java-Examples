package com.revolut.interview.payment;

import com.revolut.interview.bank.Account;
import com.revolut.interview.bank.AccountId;
import com.revolut.interview.bank.Bank;
import com.revolut.interview.bank.InsufficientFundsException;
import com.revolut.interview.bank.Money;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Kata 3: "Design a payment processing service". Flow: create (PENDING) -> fraud checks -> authorise (hold funds)
 * -> capture (settle to merchant) or refund. Idempotent create by payment id, explicit state transitions,
 * a clock for deterministic tests, and a tiny in-memory store.
 */
public final class PaymentService {
    private final Bank bank;
    private final List<FraudCheck> fraudChecks;
    private final Supplier<Instant> clock;
    private final ConcurrentHashMap<String, Payment> payments = new ConcurrentHashMap<>();

    public PaymentService(Bank bank, List<FraudCheck> fraudChecks, Supplier<Instant> clock) {
        this.bank = Objects.requireNonNull(bank);
        this.fraudChecks = List.copyOf(fraudChecks);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Idempotent: a repeated create with the same id returns the existing payment. */
    public Payment create(String paymentId, String customerId, String merchantId, Money amount) {
        return payments.computeIfAbsent(paymentId, id -> new Payment(id, customerId, merchantId, amount, PaymentStatus.PENDING, "", clock.get()));
    }

    /** PENDING -> AUTHORISED (funds moved to a hold by withdrawing from the customer) or DECLINED/FAILED. */
    public Payment authorise(String paymentId) {
        return payments.computeIfPresent(paymentId, (id, p) -> {
            if (p.status() != PaymentStatus.PENDING) throw new IllegalStateException("cannot authorise from " + p.status());
            for (FraudCheck check : fraudChecks) {
                String reason = check.evaluate(p);
                if (reason != null) return p.with(PaymentStatus.DECLINED, reason, clock.get());
            }
            Optional<Account> customer = bank.find(new AccountId(p.customerId()));
            if (customer.isEmpty()) return p.with(PaymentStatus.FAILED, "unknown customer account", clock.get());
            try {
                customer.get().withdraw(p.amount());
            } catch (InsufficientFundsException e) {
                return p.with(PaymentStatus.DECLINED, "insufficient funds", clock.get());
            }
            return p.with(PaymentStatus.AUTHORISED, "", clock.get());
        });
    }

    /** AUTHORISED -> CAPTURED: credit the merchant. */
    public Payment capture(String paymentId) {
        return payments.computeIfPresent(paymentId, (id, p) -> {
            if (p.status() != PaymentStatus.AUTHORISED) throw new IllegalStateException("cannot capture from " + p.status());
            bank.get(new AccountId(p.merchantId())).deposit(p.amount());
            return p.with(PaymentStatus.CAPTURED, "", clock.get());
        });
    }

    /** AUTHORISED (release hold) or CAPTURED (merchant pays back) -> REFUNDED. */
    public Payment refund(String paymentId) {
        return payments.computeIfPresent(paymentId, (id, p) -> {
            switch (p.status()) {
                case AUTHORISED -> bank.get(new AccountId(p.customerId())).deposit(p.amount());
                case CAPTURED -> {
                    bank.get(new AccountId(p.merchantId())).withdraw(p.amount());
                    bank.get(new AccountId(p.customerId())).deposit(p.amount());
                }
                default -> throw new IllegalStateException("cannot refund from " + p.status());
            }
            return p.with(PaymentStatus.REFUNDED, "", clock.get());
        });
    }

    public Optional<Payment> find(String paymentId) {
        return Optional.ofNullable(payments.get(paymentId));
    }
}
