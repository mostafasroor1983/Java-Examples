package module01.ddd;

import module01.immutability.Money;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentInstructionTest {
    private final Clock clock = Clock.fixed(Instant.parse("2024-03-01T10:00:00Z"), ZoneOffset.UTC);
    private final ApprovalPolicy policy = new ApprovalPolicy(Money.of("1000", "EUR"), Money.of("50000", "EUR"));
    private final UserId maker = new UserId("maker-1");

    private PaymentInstruction payment(String amount) {
        return new PaymentInstruction("P-1", Money.of(amount, "EUR"), maker, clock);
    }

    @Test
    void smallPaymentIsReleasedStraightThrough() {
        PaymentInstruction p = payment("999.99");
        p.submit(policy);
        assertEquals(PaymentInstruction.Status.RELEASED, p.status());
        List<PaymentEvent> events = p.pullEvents();
        assertInstanceOf(PaymentEvent.Submitted.class, events.get(0));
        assertInstanceOf(PaymentEvent.Released.class, events.get(1));
        assertTrue(p.pullEvents().isEmpty(), "events are drained exactly once");
    }

    @Test
    void boundaryAmountsUseInclusiveLimits() {
        assertEquals(0, policy.requiredApprovals(Money.of("1000.00", "EUR")));
        assertEquals(1, policy.requiredApprovals(Money.of("1000.01", "EUR")));
        assertEquals(1, policy.requiredApprovals(Money.of("50000", "EUR")));
        assertEquals(2, policy.requiredApprovals(Money.of("50000.01", "EUR")));
    }

    @Test
    void makerCannotApproveOwnPayment() {
        PaymentInstruction p = payment("5000");
        p.submit(policy);
        assertThrows(SeparationOfDutiesException.class, () -> p.approve(maker));
        assertEquals(PaymentInstruction.Status.PENDING_APPROVAL, p.status());
    }

    @Test
    void highValueNeedsTwoDistinctCheckers() {
        PaymentInstruction p = payment("75000");
        p.submit(policy);
        p.approve(new UserId("checker-1"));
        assertEquals(PaymentInstruction.Status.PENDING_APPROVAL, p.status());
        assertThrows(SeparationOfDutiesException.class, () -> p.approve(new UserId("checker-1")));
        p.approve(new UserId("checker-2"));
        assertEquals(PaymentInstruction.Status.RELEASED, p.status());
        assertEquals(2, p.approvers().size());
        assertThrows(UnsupportedOperationException.class, () -> p.approvers().add(new UserId("x")));
    }

    @Test
    void terminalStatesAreFinal() {
        PaymentInstruction p = payment("5000");
        p.submit(policy);
        assertThrows(IllegalArgumentException.class, () -> p.reject(new UserId("c"), " "));
        p.reject(new UserId("checker-1"), "beneficiary on watch list");
        assertThrows(IllegalStateException.class, () -> p.approve(new UserId("checker-2")));
        assertThrows(IllegalStateException.class, () -> p.submit(policy));
    }

    @Test
    void invalidConstructionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> payment("0"));
        assertThrows(IllegalArgumentException.class, () -> new UserId(" "));
        assertThrows(IllegalArgumentException.class,
                () -> new ApprovalPolicy(Money.of("10", "EUR"), Money.of("5", "EUR")));
        assertThrows(IllegalArgumentException.class,
                () -> new ApprovalPolicy(Money.of("10", "EUR"), Money.of("50", "USD")));
    }
}
