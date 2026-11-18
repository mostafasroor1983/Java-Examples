package module06.settlement;

import module06.settlement.NettingEngine.Instruction;
import module06.settlement.NettingEngine.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class NettingEngineTest {
    private static Payment p(String from, String to, String ccy, String amt) {
        return new Payment(from, to, ccy, new BigDecimal(amt));
    }

    @Test
    void netsPerCurrencyAndDropsFlatParticipants() {
        var engine = new NettingEngine();
        var positions = engine.netPositions(List.of(
                p("A", "B", "EUR", "100"), p("B", "C", "EUR", "100"), // B is flat
                p("A", "B", "USD", "50"), p("B", "A", "USD", "20")));
        assertEquals(Map.of("A", new BigDecimal("-100"), "C", new BigDecimal("100")), positions.get("EUR"));
        assertEquals(Map.of("A", new BigDecimal("-30"), "B", new BigDecimal("30")), positions.get("USD"));
        assertEquals(List.of(new Instruction("A", "C", "EUR", new BigDecimal("100")),
                new Instruction("A", "B", "USD", new BigDecimal("30"))), engine.settlementInstructions(List.of(
                p("A", "B", "EUR", "100"), p("B", "C", "EUR", "100"), p("A", "B", "USD", "50"), p("B", "A", "USD", "20"))));
    }

    @Test
    void randomisedGrossFlowsSettleToSameNetPositionsWithFewInstructions() {
        Random rnd = new Random(7);
        List<String> banks = List.of("B1", "B2", "B3", "B4", "B5");
        List<Payment> gross = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            String from = banks.get(rnd.nextInt(5)), to;
            do to = banks.get(rnd.nextInt(5)); while (to.equals(from));
            gross.add(p(from, to, i % 3 == 0 ? "USD" : "EUR", BigDecimal.valueOf(rnd.nextInt(1, 1_000_000), 2).toPlainString()));
        }
        var engine = new NettingEngine();
        List<Instruction> instructions = engine.settlementInstructions(gross);
        assertTrue(instructions.size() <= 2 * (banks.size() - 1), "at most n-1 instructions per currency");

        // applying the instructions must reproduce exactly the net positions
        Map<String, Map<String, BigDecimal>> replay = new HashMap<>();
        for (Instruction in : instructions) {
            replay.computeIfAbsent(in.currency(), k -> new HashMap<>()).merge(in.from(), in.amount().negate(), BigDecimal::add);
            replay.computeIfAbsent(in.currency(), k -> new HashMap<>()).merge(in.to(), in.amount(), BigDecimal::add);
        }
        engine.netPositions(gross).forEach((ccy, pos) -> pos.forEach((bank, net) ->
                assertEquals(0, net.compareTo(replay.get(ccy).get(bank)), ccy + " " + bank)));
    }

    @Test
    void invalidPayments() {
        assertThrows(IllegalArgumentException.class, () -> p("A", "A", "EUR", "1"));
        assertThrows(IllegalArgumentException.class, () -> p("A", "B", "EUR", "0"));
        assertTrue(new NettingEngine().settlementInstructions(List.of()).isEmpty());
    }
}
