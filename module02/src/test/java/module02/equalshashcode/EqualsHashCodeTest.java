package module02.equalshashcode;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EqualsHashCodeTest {

    @Test
    void brokenAccountIsEqualButNotFoundInHashSet() {
        BrokenAccount a = new BrokenAccount("GB00");
        BrokenAccount b = new BrokenAccount("GB00");
        assertEquals(a, b);
        Set<BrokenAccount> set = new HashSet<>();
        set.add(a);
        // Identity hash codes almost never collide, so contains() lies.
        if (a.hashCode() != b.hashCode()) {
            assertFalse(set.contains(b));
        }
    }

    @Test
    void asymmetricEqualsViolatesContract() {
        AsymmetricCard plain = new AsymmetricCard("4111");
        AsymmetricCard scheme = new AsymmetricCard.SchemeCard("4111", "VISA");
        assertTrue(plain.equals(scheme));
        assertFalse(scheme.equals(plain), "symmetry broken: a.equals(b) but !b.equals(a)");
    }

    @Test
    void goodAccountHonoursContract() {
        Account a = new Account("GB00", "Alice");
        Account b = new Account("GB00", "Alice Smith"); // same identity, different display name
        Account c = new Account("GB01", "Alice");
        // reflexive, symmetric, consistent with hashCode
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "GB00");
        Set<Account> set = new HashSet<>(Set.of(a));
        assertTrue(set.contains(b));
    }
}
