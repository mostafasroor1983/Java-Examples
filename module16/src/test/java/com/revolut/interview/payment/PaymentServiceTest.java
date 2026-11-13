package com.revolut.interview.payment;

import com.revolut.interview.bank.AccountId;
import com.revolut.interview.bank.Bank;
import com.revolut.interview.bank.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class PaymentServiceTest {
    private static final Currency GBP = Currency.getInstance("GBP");
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
    private Bank bank;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        bank = new Bank();
        bank.open(new AccountId("cust"), GBP).deposit(Money.of("100", "GBP"));
        bank.open(new AccountId("merch"), GBP);
        FraudCheck bigAmount = p -> p.amount().compareTo(Money.of("1000", "GBP")) > 0 ? "amount too large" : null;
        FraudCheck blockedMerchant = p -> p.merchantId().equals("blocked") ? "merchant blocked" : null;
        service = new PaymentService(bank, List.of(bigAmount, blockedMerchant), now::get);
    }

    @Test
    void authoriseCaptureFlowMovesMoneyAndTimestamps() {
        service.create("p1", "cust", "merch", Money.of("40", "GBP"));
        assertThat(service.create("p1", "cust", "merch", Money.of("999", "GBP")).amount()).as("idempotent create").isEqualTo(Money.of("40", "GBP"));
        now.set(Instant.EPOCH.plusSeconds(1));
        Payment auth = service.authorise("p1");
        assertThat(auth.status()).isEqualTo(PaymentStatus.AUTHORISED);
        assertThat(auth.updatedAt()).isEqualTo(Instant.EPOCH.plusSeconds(1));
        assertThat(bank.get(new AccountId("cust")).balance()).isEqualTo(Money.of("60", "GBP"));
        assertThat(bank.get(new AccountId("merch")).balance()).isEqualTo(Money.of("0", "GBP"));
        assertThat(service.capture("p1").status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(bank.get(new AccountId("merch")).balance()).isEqualTo(Money.of("40", "GBP"));
        assertThat(service.refund("p1").status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(bank.get(new AccountId("cust")).balance()).isEqualTo(Money.of("100", "GBP"));
        assertThat(bank.get(new AccountId("merch")).balance()).isEqualTo(Money.of("0", "GBP"));
    }

    @Test
    void declinesAndFailures() {
        service.create("big", "cust", "merch", Money.of("5000", "GBP"));
        assertThat(service.authorise("big")).extracting(Payment::status, Payment::reason).containsExactly(PaymentStatus.DECLINED, "amount too large");
        service.create("blocked", "cust", "blocked", Money.of("5", "GBP"));
        assertThat(service.authorise("blocked").reason()).isEqualTo("merchant blocked");
        service.create("poor", "cust", "merch", Money.of("500", "GBP"));
        assertThat(service.authorise("poor").reason()).isEqualTo("insufficient funds");
        service.create("ghost", "nobody", "merch", Money.of("5", "GBP"));
        assertThat(service.authorise("ghost").status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(bank.get(new AccountId("cust")).balance()).as("declines never move money").isEqualTo(Money.of("100", "GBP"));
    }

    @Test
    void illegalTransitionsAreRejected() {
        service.create("p", "cust", "merch", Money.of("1", "GBP"));
        assertThatThrownBy(() -> service.capture("p")).isInstanceOf(IllegalStateException.class).hasMessageContaining("PENDING");
        assertThatThrownBy(() -> service.refund("p")).isInstanceOf(IllegalStateException.class);
        service.authorise("p");
        assertThatThrownBy(() -> service.authorise("p")).isInstanceOf(IllegalStateException.class);
        assertThat(service.refund("p").status()).as("refund from AUTHORISED releases the hold").isEqualTo(PaymentStatus.REFUNDED);
        assertThat(bank.get(new AccountId("cust")).balance()).isEqualTo(Money.of("100", "GBP"));
        assertThat(service.find("missing")).isEmpty();
        assertThat(service.authorise("missing")).isNull();
        assertThatThrownBy(() -> new Payment("x", "c", "m", Money.of("0", "GBP"), PaymentStatus.PENDING, "", Instant.EPOCH)).isInstanceOf(IllegalArgumentException.class);
    }
}
