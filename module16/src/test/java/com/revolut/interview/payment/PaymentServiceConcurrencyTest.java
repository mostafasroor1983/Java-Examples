package com.revolut.interview.payment;

import com.revolut.interview.bank.AccountId;
import com.revolut.interview.bank.Bank;
import com.revolut.interview.bank.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Races on the same payment and on the same customer's funds. computeIfPresent serialises per payment id. */
class PaymentServiceConcurrencyTest {
    private static final Currency GBP = Currency.getInstance("GBP");

    private static List<Future<Object>> race(int threads, Callable<Object> body) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return body.call();
            }));
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        return results;
    }

    private static long successes(List<Future<Object>> results) {
        return results.stream().filter(f -> {
            try {
                f.get();
                return true;
            } catch (Exception e) {
                return false;
            }
        }).count();
    }

    @Test
    void concurrentCapturesOfSamePaymentCreditMerchantExactlyOnce() throws Exception {
        Bank bank = new Bank();
        bank.open(new AccountId("cust"), GBP).deposit(Money.of("100", "GBP"));
        bank.open(new AccountId("merch"), GBP);
        PaymentService service = new PaymentService(bank, List.of(), () -> Instant.EPOCH);
        service.create("p1", "cust", "merch", Money.of("40", "GBP"));
        service.authorise("p1");

        var results = race(16, () -> service.capture("p1"));
        assertThat(successes(results)).as("exactly one capture wins, the rest see CAPTURED and throw").isEqualTo(1);
        assertThat(bank.get(new AccountId("merch")).balance()).isEqualTo(Money.of("40", "GBP"));

        var refunds = race(16, () -> service.refund("p1"));
        assertThat(successes(refunds)).isEqualTo(1);
        assertThat(bank.get(new AccountId("cust")).balance()).isEqualTo(Money.of("100", "GBP"));
        assertThat(bank.get(new AccountId("merch")).balance()).isEqualTo(Money.of("0", "GBP"));
    }

    @Test
    void parallelAuthorisationsNeverOverdrawTheCustomer() throws Exception {
        Bank bank = new Bank();
        bank.open(new AccountId("cust"), GBP).deposit(Money.of("100", "GBP"));
        bank.open(new AccountId("merch"), GBP);
        PaymentService service = new PaymentService(bank, List.of(), () -> Instant.EPOCH);
        for (int i = 0; i < 50; i++) service.create("p" + i, "cust", "merch", Money.of("7", "GBP"));

        int[] next = {0};
        race(50, () -> {
            int id;
            synchronized (next) {
                id = next[0]++;
            }
            return service.authorise("p" + id);
        });
        long authorised = 0;
        for (int i = 0; i < 50; i++) {
            if (service.find("p" + i).orElseThrow().status() == PaymentStatus.AUTHORISED) authorised++;
        }
        assertThat(authorised).as("floor(100 / 7)").isEqualTo(14);
        assertThat(bank.get(new AccountId("cust")).balance()).isEqualTo(Money.of("2", "GBP"));
    }
}
