package module05.repository;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {
    private static Payment pay(String id, String cust, String amt, Payment.Status s) {
        return new Payment(new PaymentId(id), cust, new BigDecimal(amt), s);
    }

    @Test
    void crudAndDomainQueries() {
        PaymentRepository repo = new PaymentRepository();
        repo.save(pay("p1", "c1", "10", Payment.Status.SETTLED));
        repo.save(pay("p2", "c1", "5", Payment.Status.PENDING));
        repo.save(pay("p3", "c2", "7", Payment.Status.SETTLED));
        assertEquals(3, repo.count());
        assertEquals(2, repo.findByCustomer("c1").size());
        assertEquals(new BigDecimal("17"), repo.settledTotal());
        assertTrue(repo.findById(new PaymentId("p2")).isPresent());
        repo.save(repo.findById(new PaymentId("p2")).orElseThrow().withStatus(Payment.Status.SETTLED));
        assertEquals(new BigDecimal("22"), repo.settledTotal());
        assertTrue(repo.deleteById(new PaymentId("p3")));
        assertFalse(repo.deleteById(new PaymentId("p3")));
        assertEquals(2, repo.findAll().size());
    }

    @Test
    void validation() {
        PaymentRepository repo = new PaymentRepository();
        assertThrows(NullPointerException.class, () -> repo.save(null));
        assertThrows(IllegalArgumentException.class, () -> pay("p", "c", "0", Payment.Status.PENDING));
        assertThrows(IllegalArgumentException.class, () -> new PaymentId(" "));
    }

    @Test
    void genericRepositoryWorksForAnotherEntity() {
        record Customer(String id, String name) implements Entity<String> {
        }
        Repository<Customer, String> repo = new InMemoryRepository<>();
        repo.save(new Customer("c1", "Ada"));
        assertEquals("Ada", repo.findById("c1").orElseThrow().name());
        assertEquals(1, repo.findWhere(c -> c.name().startsWith("A")).size());
    }

    @Test
    void concurrentSavesAreAllVisible() throws Exception {
        PaymentRepository repo = new PaymentRepository();
        int threads = 8, per = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            int tt = t;
            pool.submit(() -> {
                try {
                    for (int i = 0; i < per; i++) repo.save(pay("p" + tt + "-" + i, "c", "1", Payment.Status.SETTLED));
                } finally {
                    done.countDown();
                }
            });
        }
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(threads * per, repo.count());
        assertEquals(new BigDecimal(threads * per), repo.settledTotal());
    }
}
