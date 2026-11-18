package module01.ddd;

import module01.immutability.Money;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregate root for an outgoing payment that may need maker-checker approval.
 *
 * <p>Why this matters: regulators (and internal audit) expect that no single employee can both create
 * and release a high-value payment. Putting that rule <em>inside the aggregate</em> means no service,
 * controller or batch job can bypass it by forgetting a check.
 *
 * <p>Invariants enforced here:
 * <ol>
 *   <li>The maker can never approve their own payment.</li>
 *   <li>A checker can approve a given payment at most once.</li>
 *   <li>Only DRAFT can be submitted; only PENDING_APPROVAL can be approved or rejected.</li>
 *   <li>Terminal states (RELEASED, REJECTED) are final.</li>
 * </ol>
 *
 * <p>Concurrency: an aggregate is NOT thread-safe by design. It is loaded, mutated and saved inside one
 * transaction; concurrent modification is detected by the repository (optimistic version column),
 * not by locks in the domain object. See {@code module01.hexagonal}.
 */
public final class PaymentInstruction {
    public enum Status { DRAFT, PENDING_APPROVAL, RELEASED, REJECTED }

    private final String id;
    private final Money amount;
    private final UserId maker;
    private final Clock clock;
    private final Set<UserId> approvers = new LinkedHashSet<>();
    private final List<PaymentEvent> pendingEvents = new ArrayList<>();
    private Status status = Status.DRAFT;
    private int approvalsRequired = -1;

    public PaymentInstruction(String id, Money amount, UserId maker, Clock clock) {
        this.id = Objects.requireNonNull(id, "id");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.maker = Objects.requireNonNull(maker, "maker");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (amount.isNegative() || amount.amount().signum() == 0) {
            throw new IllegalArgumentException("payment amount must be positive");
        }
    }

    public void submit(ApprovalPolicy policy) {
        requireStatus(Status.DRAFT, "submit");
        approvalsRequired = policy.requiredApprovals(amount);
        status = Status.PENDING_APPROVAL;
        pendingEvents.add(new PaymentEvent.Submitted(id, maker, amount, approvalsRequired, clock.instant()));
        if (approvalsRequired == 0) release();
    }

    public void approve(UserId checker) {
        Objects.requireNonNull(checker, "checker");
        requireStatus(Status.PENDING_APPROVAL, "approve");
        if (checker.equals(maker)) {
            throw new SeparationOfDutiesException("maker " + maker.value() + " cannot approve own payment " + id);
        }
        if (!approvers.add(checker)) {
            throw new SeparationOfDutiesException(checker.value() + " already approved payment " + id);
        }
        pendingEvents.add(new PaymentEvent.ApprovalRecorded(id, checker, approvers.size(), clock.instant()));
        if (approvers.size() >= approvalsRequired) release();
    }

    public void reject(UserId checker, String reason) {
        Objects.requireNonNull(checker, "checker");
        requireStatus(Status.PENDING_APPROVAL, "reject");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("a rejection needs a reason");
        status = Status.REJECTED;
        pendingEvents.add(new PaymentEvent.Rejected(id, checker, reason, clock.instant()));
    }

    /**
     * Returns and clears the events raised since the last call. In production the repository persists
     * them in the same DB transaction as the aggregate (transactional outbox) so state and events never diverge.
     */
    public List<PaymentEvent> pullEvents() {
        List<PaymentEvent> out = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return out;
    }

    private void release() {
        status = Status.RELEASED;
        pendingEvents.add(new PaymentEvent.Released(id, clock.instant()));
    }

    private void requireStatus(Status expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("cannot " + action + " payment " + id + " in status " + status);
        }
    }

    public String id() {
        return id;
    }

    public Money amount() {
        return amount;
    }

    public Status status() {
        return status;
    }

    public Set<UserId> approvers() {
        return Set.copyOf(approvers); // defensive copy: callers cannot add a fake approver
    }
}
