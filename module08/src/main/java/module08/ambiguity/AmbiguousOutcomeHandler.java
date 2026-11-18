package module08.ambiguity;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * "The request timed out" does NOT mean "the payment failed". The provider may have executed it and the response was
 * lost. Blindly resubmitting risks paying twice; blindly reporting failure leaves money in flight.
 *
 * <p>Protocol implemented here:
 * <ol>
 *   <li>Submit with an idempotency key.</li>
 *   <li>On timeout, query status by the same key, up to {@code statusChecks} times.</li>
 *   <li>Provider says ACCEPTED/REJECTED: that is the answer.</li>
 *   <li>Provider says NOT_FOUND: the request never arrived, so it is safe to resubmit ONCE with the SAME key.</li>
 *   <li>Still unknown: return {@link Outcome.Unknown}. The payment is parked for reconciliation against the
 *   provider's settlement file, and is never retried as a new payment.</li>
 * </ol>
 */
public final class AmbiguousOutcomeHandler {

    public enum ProviderStatus { ACCEPTED, REJECTED, NOT_FOUND, PROCESSING }

    public interface ProviderClient {
        ProviderStatus submit(String idempotencyKey) throws TimeoutException;

        ProviderStatus status(String idempotencyKey) throws TimeoutException;
    }

    public sealed interface Outcome {
        record Accepted(String key) implements Outcome { }

        record Rejected(String key) implements Outcome { }

        /** Requires reconciliation: do not tell the customer it failed, do not retry as a new payment. */
        record Unknown(String key, String lastKnown) implements Outcome { }
    }

    private final ProviderClient client;
    private final int statusChecks;

    public AmbiguousOutcomeHandler(ProviderClient client, int statusChecks) {
        this.client = Objects.requireNonNull(client, "client");
        if (statusChecks < 1) throw new IllegalArgumentException("statusChecks must be >= 1");
        this.statusChecks = statusChecks;
    }

    public Outcome submit(String key) {
        Objects.requireNonNull(key, "key");
        try {
            return terminal(key, client.submit(key));
        } catch (TimeoutException firstTimeout) {
            return resolve(key, true);
        }
    }

    private Outcome resolve(String key, boolean mayResubmit) {
        String lastKnown = "TIMEOUT";
        for (int i = 0; i < statusChecks; i++) {
            ProviderStatus st;
            try {
                st = client.status(key);
            } catch (TimeoutException e) {
                lastKnown = "STATUS_TIMEOUT";
                continue;
            }
            switch (st) {
                case ACCEPTED, REJECTED -> {
                    return terminal(key, st);
                }
                case NOT_FOUND -> {
                    if (!mayResubmit) return new Outcome.Unknown(key, "NOT_FOUND_AFTER_RESUBMIT");
                    try {
                        return terminal(key, client.submit(key)); // same key: provider dedups if it did arrive meanwhile
                    } catch (TimeoutException e) {
                        return resolve(key, false);
                    }
                }
                case PROCESSING -> lastKnown = "PROCESSING";
            }
        }
        return new Outcome.Unknown(key, lastKnown);
    }

    private static Outcome terminal(String key, ProviderStatus st) {
        return switch (st) {
            case ACCEPTED -> new Outcome.Accepted(key);
            case REJECTED -> new Outcome.Rejected(key);
            case NOT_FOUND, PROCESSING -> new Outcome.Unknown(key, st.name());
        };
    }
}
