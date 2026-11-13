package module05.wildcards;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WildcardsTest {

    @Test
    void producerExtendsAcceptsSubtypeLists() {
        List<Wildcards.CardTx> cards = List.of(new Wildcards.CardTx(BigDecimal.ONE), new Wildcards.CardTx(BigDecimal.TEN));
        List<Wildcards.Transaction> mixed = List.of(new Wildcards.BankTx(new BigDecimal("5")));
        assertEquals(new BigDecimal("11"), Wildcards.total(cards));
        assertEquals(new BigDecimal("5"), Wildcards.total(mixed));
    }

    @Test
    void consumerSuperAcceptsSupertypeSinks() {
        List<Object> objects = new ArrayList<>();
        List<Wildcards.Transaction> txs = new ArrayList<>();
        Wildcards.addCardTxs(objects, 2);
        Wildcards.addCardTxs(txs, 3);
        assertEquals(2, objects.size());
        assertEquals(3, txs.size());
    }

    @Test
    void copyAndWiden() {
        List<Wildcards.CardTx> src = List.of(new Wildcards.CardTx(BigDecimal.ONE));
        List<Wildcards.Transaction> dest = new ArrayList<>();
        Wildcards.copy(dest, src);
        assertEquals(1, dest.size());
        List<Wildcards.Transaction> widened = Wildcards.widen(src);
        widened.add(new Wildcards.BankTx(BigDecimal.ONE)); // legal on the widened copy
        assertEquals(2, widened.size());
        assertEquals(2, Wildcards.countNonNull(Arrays.asList("a", null, "b")));
    }
}
