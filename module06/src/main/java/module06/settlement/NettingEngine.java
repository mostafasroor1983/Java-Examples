package module06.settlement;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Multilateral netting, as done by clearing houses and for intra-group settlement: instead of settling every gross
 * payment, compute each participant's NET position per currency and settle only the differences.
 * 10,000 gross payments between 5 banks collapse to at most 4 settlement transfers per currency.
 *
 * <p>Stream techniques: {@code flatMap} (one payment becomes two signed legs), nested {@code groupingBy} with
 * {@code TreeMap} factories (deterministic ordering for audit), {@code reducing(ZERO, BigDecimal::add)} for exact sums
 * (never {@code mapToDouble().sum()}), and a zero-sum invariant check per currency.
 */
public final class NettingEngine {
    public record Payment(String payer, String payee, String currency, BigDecimal amount) {
        public Payment {
            Objects.requireNonNull(payer);
            Objects.requireNonNull(payee);
            Objects.requireNonNull(currency);
            Objects.requireNonNull(amount);
            if (amount.signum() <= 0) throw new IllegalArgumentException("gross payments must be positive");
            if (payer.equals(payee)) throw new IllegalArgumentException("self payment");
        }
    }

    public record Instruction(String from, String to, String currency, BigDecimal amount) {
    }

    private record Leg(String participant, String currency, BigDecimal signed) {
    }

    /** currency -> participant -> net position (positive = receives, negative = pays). Zero positions removed. */
    public Map<String, Map<String, BigDecimal>> netPositions(List<Payment> payments) {
        Map<String, Map<String, BigDecimal>> positions = payments.stream()
                .flatMap(p -> Stream.of(
                        new Leg(p.payer(), p.currency(), p.amount().negate()),
                        new Leg(p.payee(), p.currency(), p.amount())))
                .collect(Collectors.groupingBy(Leg::currency, TreeMap::new,
                        Collectors.groupingBy(Leg::participant, TreeMap::new,
                                Collectors.reducing(BigDecimal.ZERO, Leg::signed, BigDecimal::add))));

        positions.forEach((ccy, byParticipant) -> {
            BigDecimal sum = byParticipant.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.signum() != 0) throw new IllegalStateException("netting does not balance for " + ccy + ": " + sum);
            byParticipant.values().removeIf(v -> v.signum() == 0);
        });
        return positions;
    }

    /**
     * Greedy settlement: match the largest payer with the largest receiver until all positions are flat.
     * Produces at most (participants - 1) instructions per currency. Deterministic ordering for reproducibility.
     */
    public List<Instruction> settlementInstructions(List<Payment> payments) {
        List<Instruction> out = new ArrayList<>();
        netPositions(payments).forEach((ccy, positions) -> {
            Comparator<Map.Entry<String, BigDecimal>> bySize =
                    Comparator.comparing((Map.Entry<String, BigDecimal> e) -> e.getValue().abs()).reversed()
                            .thenComparing(Map.Entry::getKey);
            Deque<Map.Entry<String, BigDecimal>> payers = positions.entrySet().stream()
                    .filter(e -> e.getValue().signum() < 0).sorted(bySize)
                    .map(e -> Map.entry(e.getKey(), e.getValue().negate()))
                    .collect(Collectors.toCollection(ArrayDeque::new));
            Deque<Map.Entry<String, BigDecimal>> receivers = positions.entrySet().stream()
                    .filter(e -> e.getValue().signum() > 0).sorted(bySize)
                    .collect(Collectors.toCollection(ArrayDeque::new));
            while (!payers.isEmpty()) {
                var p = payers.poll();
                var r = receivers.poll();
                BigDecimal amt = p.getValue().min(r.getValue());
                out.add(new Instruction(p.getKey(), r.getKey(), ccy, amt));
                if (p.getValue().compareTo(amt) > 0) payers.addFirst(Map.entry(p.getKey(), p.getValue().subtract(amt)));
                if (r.getValue().compareTo(amt) > 0) receivers.addFirst(Map.entry(r.getKey(), r.getValue().subtract(amt)));
            }
        });
        return List.copyOf(out);
    }
}
