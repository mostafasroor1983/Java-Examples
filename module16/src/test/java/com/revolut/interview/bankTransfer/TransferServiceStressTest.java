package com.revolut.interview.bankTransfer;

import com.revolut.interview.bank.Account;
import com.revolut.interview.bank.AccountId;
import com.revolut.interview.bank.Bank;
import com.revolut.interview.bank.Money;
import com.revolut.interview.bankTransfer.TransferService.Status;
import com.revolut.interview.bankTransfer.TransferService.TransferRequest;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stress test: random transfers across many accounts from many threads, with every request submitted twice
 * (simulating client retries), repeated a few times to shake out rare interleavings.
 * Invariants: money conserved, no negative balance, each request id completes at most once,
 * and every account's transaction history replays exactly to its balance (audit trail is complete).
 */
class TransferServiceStressTest {
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final int ACCOUNTS = 6, THREADS = 8, PER_THREAD = 1_500;

    @RepeatedTest(3)
    void randomTransfersWithRetriesPreserveAllInvariants() throws Exception {
        Bank bank = new Bank();
        List<AccountId> ids = new ArrayList<>();
        for (int i = 0; i < ACCOUNTS; i++) {
            AccountId id = new AccountId("ACC-" + i);
            bank.open(id, EUR).deposit(Money.of("1000.00", "EUR"));
            ids.add(id);
        }
        TransferService service = new TransferService(bank);
        Map<String, AtomicInteger> completedPerRequest = new ConcurrentHashMap<>();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        for (int t = 0; t < THREADS; t++) {
            int tid = t;
            pool.submit(() -> {
                start.await();
                var rnd = ThreadLocalRandom.current();
                for (int i = 0; i < PER_THREAD; i++) {
                    AccountId from = ids.get(rnd.nextInt(ACCOUNTS)), to = ids.get(rnd.nextInt(ACCOUNTS));
                    var req = new TransferRequest("req-" + tid + "-" + i, from, to,
                            Money.of(rnd.nextInt(1, 30_000) / 100 + "." + String.format("%02d", rnd.nextInt(100)), "EUR"));
                    for (int attempt = 0; attempt < 2; attempt++) { // client retry with the same request id
                        if (service.transfer(req).status() == Status.COMPLETED) {
                            completedPerRequest.computeIfAbsent(req.requestId(), k -> new AtomicInteger()).incrementAndGet();
                        }
                    }
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).as("no deadlock").isTrue();

        Money total = Money.zero(EUR);
        for (AccountId id : ids) {
            Account acc = bank.get(id);
            assertThat(acc.balance().isNegative()).as("no overdraft on " + id.value()).isFalse();
            Money replayed = Money.zero(EUR);
            for (Account.Transaction tx : acc.transactions()) {
                replayed = tx.kind() == Account.Kind.DEPOSIT ? replayed.plus(tx.amount()) : replayed.minus(tx.amount());
                assertThat(tx.balanceAfter()).as("running balance recorded correctly").isEqualTo(replayed);
            }
            assertThat(replayed).as("history explains balance of " + id.value()).isEqualTo(acc.balance());
            total = total.plus(acc.balance());
        }
        assertThat(total).as("money conserved").isEqualTo(Money.of("6000.00", "EUR"));
        assertThat(completedPerRequest.values()).allSatisfy(c -> assertThat(c.get()).isEqualTo(1));
    }
}
