package com.revolut.interview.bank;

import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class AccountTest {
    private static final Currency GBP = Currency.getInstance("GBP");

    @Test
    void depositsAndWithdrawalsKeepInvariantsAndHistory() {
        Account acc = new Account(new AccountId("A"), GBP);
        acc.deposit(Money.of("100", "GBP"));
        acc.withdraw(Money.of("40", "GBP"));
        assertThat(acc.balance()).isEqualTo(Money.of("60.00", "GBP"));
        assertThat(acc.transactions()).hasSize(2)
                .extracting(Account.Transaction::kind)
                .containsExactly(Account.Kind.DEPOSIT, Account.Kind.WITHDRAWAL);
        assertThatThrownBy(() -> acc.withdraw(Money.of("60.01", "GBP")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("requested 60.01");
        assertThat(acc.balance()).isEqualTo(Money.of("60", "GBP"));
    }

    @Test
    void validation() {
        Account acc = new Account(new AccountId("A"), GBP);
        assertThatThrownBy(() -> acc.deposit(Money.of("0", "GBP"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> acc.deposit(Money.of("1", "EUR"))).isInstanceOf(Money.CurrencyMismatchException.class);
        assertThatThrownBy(() -> new AccountId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(Money.of("1.0", "GBP")).isEqualTo(Money.of("1.00", "GBP"));
        assertThatThrownBy(() -> acc.transactions().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void bankRegistry() {
        Bank bank = new Bank();
        bank.open(new AccountId("A"), GBP);
        assertThatThrownBy(() -> bank.open(new AccountId("A"), GBP)).isInstanceOf(IllegalStateException.class);
        assertThat(bank.find(new AccountId("Z"))).isEmpty();
        assertThatThrownBy(() -> bank.get(new AccountId("Z"))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Z");
        assertThat(bank.size()).isEqualTo(1);
    }

    @Test
    void concurrentDepositsAreExact() throws Exception {
        Account acc = new Account(new AccountId("A"), GBP);
        int threads = 10, per = 1000;
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < per; i++) acc.deposit(Money.of("0.01", "GBP"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(acc.balance()).isEqualTo(Money.of("100.00", "GBP"));
        assertThat(acc.transactions()).hasSize(threads * per);
    }
}
