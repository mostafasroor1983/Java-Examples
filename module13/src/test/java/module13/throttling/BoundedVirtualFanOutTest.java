package module13.throttling;

import module13.throttling.BoundedVirtualFanOut.Outcome;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class BoundedVirtualFanOutTest {

    private static String statementFor(int account) {
        try {
            Thread.sleep(5); // simulated DB query: the virtual thread unmounts here
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        if (account % 1_000 == 999) throw new IllegalStateException("corrupt row for account " + account);
        return "STMT-" + account;
    }

    @Test
    void thousandsOfAccountsNeverExceedConnectionPoolSize() throws Exception {
        var fanOut = new BoundedVirtualFanOut(20, Duration.ofMinutes(1));
        List<Integer> accounts = IntStream.range(0, 5_000).boxed().toList();
        var outcomes = fanOut.run(accounts, BoundedVirtualFanOutTest::statementFor);

        assertEquals(5_000, outcomes.size());
        assertTrue(fanOut.maxObservedInFlight() <= 20, "admission control held: " + fanOut.maxObservedInFlight());
        assertTrue(fanOut.maxObservedInFlight() > 1, "work actually ran concurrently");
        assertEquals(new Outcome.Ok<>("STMT-0"), outcomes.get(0));
        List<Integer> failed = BoundedVirtualFanOut.failedKeys(outcomes);
        assertEquals(List.of(999, 1999, 2999, 3999, 4999), failed, "failures isolated and reported for rerun");
        assertEquals(new Outcome.Failed<>("IllegalStateException"), outcomes.get(999), "no customer data in reason");
        assertEquals(List.copyOf(outcomes.keySet()), accounts, "input order preserved");
    }

    @Test
    void admissionTimeoutShedsLoadInsteadOfQueueingForever() throws Exception {
        var fanOut = new BoundedVirtualFanOut(1, Duration.ofMillis(20));
        var outcomes = fanOut.run(List.of(1, 2, 3, 4, 5), k -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return k;
        });
        long shed = outcomes.values().stream().filter(o -> o.equals(new Outcome.Failed<>("ADMISSION_TIMEOUT"))).count();
        assertEquals(4, shed);
        assertEquals(1, outcomes.size() - shed);
    }

    @Test
    void invalidConfig() {
        assertThrows(IllegalArgumentException.class, () -> new BoundedVirtualFanOut(0, Duration.ofSeconds(1)));
    }
}
