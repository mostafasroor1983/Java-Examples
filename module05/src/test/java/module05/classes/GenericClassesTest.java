package module05.classes;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GenericClassesTest {

    @Test
    void pairIsTypedAndSwappable() {
        Pair<String, Integer> p = Pair.of("GBP", 1);
        Pair<Integer, String> s = p.swap();
        assertEquals(1, s.first());
        assertEquals("GBP", s.second());
        assertEquals(Pair.of("GBP", 1), p);
        assertEquals(Pair.of("GBP", 1).hashCode(), p.hashCode());
        assertThrows(NullPointerException.class, () -> Pair.of(null, 1));
    }

    @Test
    void phantomTypeIdsAreDistinctAtCompileTimeButEqualByValue() {
        UUID raw = UUID.randomUUID();
        TypedId<Account> accountId = TypedId.of(raw);
        TypedId<Wallet> walletId = TypedId.of(raw);
        // Account a = new Account(walletId, "GB00"); // does not compile: TypedId<Wallet> != TypedId<Account>
        Account a = new Account(accountId, "GB00");
        assertEquals(accountId, a.id());
        assertEquals(accountId, walletId, "erasure: same runtime value => equal");
        assertNotEquals(TypedId.<Wallet>random(), walletId);
    }
}
