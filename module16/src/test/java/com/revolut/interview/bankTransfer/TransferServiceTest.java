package com.revolut.interview.bankTransfer;

import com.revolut.interview.bank.AccountId;
import com.revolut.interview.bank.Bank;
import com.revolut.interview.bank.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class TransferServiceTest {
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final AccountId A = new AccountId("A"), B = new AccountId("B"), C = new AccountId("C");
    private Bank bank;
    private TransferService service;

    @BeforeEach
    void setUp() {
        bank = new Bank();
        for (AccountId id : new AccountId[]{A, B, C}) bank.open(id, GBP).deposit(Money.of("1000", "GBP"));
        service = new TransferService(bank);
    }

    private TransferService.TransferRequest req(String id, AccountId from, AccountId to, String amount) {
        return new TransferService.TransferRequest(id, from, to, Money.of(amount, "GBP"));
    }

    @Test
    void completesAndIsIdempotent() {
        var r1 = service.transfer(req("r1", A, B, "250"));
        assertThat(r1.status()).isEqualTo(TransferService.Status.COMPLETED);
        assertThat(bank.get(A).balance()).isEqualTo(Money.of("750", "GBP"));
        assertThat(bank.get(B).balance()).isEqualTo(Money.of("1250", "GBP"));
        var dup = service.transfer(req("r1", A, B, "250"));
        assertThat(dup.status()).isEqualTo(TransferService.Status.DUPLICATE);
        assertThat(bank.get(A).balance()).isEqualTo(Money.of("750", "GBP"));
    }

    @Test
    void rejectionsLeaveBalancesUntouched() {
        assertThat(service.transfer(req("r2", A, B, "5000")).status()).isEqualTo(TransferService.Status.INSUFFICIENT_FUNDS);
        assertThat(service.transfer(req("r3", A, A, "1")).status()).isEqualTo(TransferService.Status.REJECTED);
        assertThat(service.transfer(req("r4", A, new AccountId("Z"), "1")).status()).isEqualTo(TransferService.Status.REJECTED);
        assertThat(service.transfer(new TransferService.TransferRequest("r5", A, B, Money.of("1", "EUR"))).message()).contains("currency");
        assertThat(service.transfer(req("r6", A, B, "0")).status()).isEqualTo(TransferService.Status.REJECTED);
        assertThat(bank.get(A).balance()).isEqualTo(Money.of("1000", "GBP"));
        assertThat(bank.get(B).balance()).isEqualTo(Money.of("1000", "GBP"));
    }

    @Test
    void opposingConcurrentTransfersNeverDeadlockAndConserveMoney() throws Exception {
        AccountId[][] pairs = {{A, B}, {B, A}, {B, C}, {C, B}, {C, A}, {A, C}};
        int perThread = 1000;
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(pairs.length);
        for (AccountId[] p : pairs) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        service.transfer(req(p[0].value() + p[1].value() + i, p[0], p[1], "1"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).as("no deadlock").isTrue();
        Money total = bank.get(A).balance().plus(bank.get(B).balance()).plus(bank.get(C).balance());
        assertThat(total).isEqualTo(Money.of("3000", "GBP"));
        assertThat(bank.get(A).balance().isNegative()).isFalse();
        assertThat(bank.get(B).balance().isNegative()).isFalse();
        assertThat(bank.get(C).balance().isNegative()).isFalse();
    }
}
