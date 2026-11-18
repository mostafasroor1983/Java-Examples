package module08.ambiguity;

import module08.ambiguity.AmbiguousOutcomeHandler.Outcome;
import module08.ambiguity.AmbiguousOutcomeHandler.ProviderClient;
import module08.ambiguity.AmbiguousOutcomeHandler.ProviderStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class AmbiguousOutcomeHandlerTest {

    /** Scripted fake: submit responses are consumed in order, and a null entry means "this call times out". */
    static final class Scripted implements ProviderClient {
        private final List<ProviderStatus> submitScript;
        private final Deque<ProviderStatus> statuses;
        int submitCalls;

        Scripted(List<ProviderStatus> submitScript, List<ProviderStatus> statuses) {
            this.submitScript = submitScript;
            this.statuses = new ArrayDeque<>(statuses);
        }

        public ProviderStatus submit(String key) throws TimeoutException {
            ProviderStatus s = submitScript.get(submitCalls++);
            if (s == null) throw new TimeoutException("read timed out");
            return s;
        }

        public ProviderStatus status(String key) {
            return statuses.isEmpty() ? ProviderStatus.PROCESSING : statuses.poll();
        }
    }

    @Test
    void directAnswer() {
        var h = new AmbiguousOutcomeHandler(new Scripted(List.of(ProviderStatus.ACCEPTED), List.of()), 3);
        assertEquals(new Outcome.Accepted("k"), h.submit("k"));
    }

    @Test
    void timeoutButProviderActuallyExecutedIt() {
        var client = new Scripted(java.util.Arrays.asList((ProviderStatus) null), List.of(ProviderStatus.PROCESSING, ProviderStatus.ACCEPTED));
        assertEquals(new Outcome.Accepted("k"), new AmbiguousOutcomeHandler(client, 3).submit("k"));
        assertEquals(1, client.submitCalls, "never resubmitted: would have paid twice with a non-idempotent provider");
    }

    @Test
    void notFoundIsResubmittedOnceWithSameKey() {
        var client = new Scripted(java.util.Arrays.asList(null, ProviderStatus.REJECTED), List.of(ProviderStatus.NOT_FOUND));
        assertEquals(new Outcome.Rejected("k"), new AmbiguousOutcomeHandler(client, 3).submit("k"));
        assertEquals(2, client.submitCalls);
    }

    @Test
    void stillUnknownIsParkedNotFailed() {
        var client = new Scripted(java.util.Arrays.asList((ProviderStatus) null), List.of());
        Outcome o = new AmbiguousOutcomeHandler(client, 2).submit("k");
        assertEquals(new Outcome.Unknown("k", "PROCESSING"), o);
        assertEquals(1, client.submitCalls);
    }

    @Test
    void secondTimeoutAfterResubmitStopsResubmitting() {
        var client = new Scripted(java.util.Arrays.asList(null, null),
                List.of(ProviderStatus.NOT_FOUND, ProviderStatus.NOT_FOUND));
        assertEquals(new Outcome.Unknown("k", "NOT_FOUND_AFTER_RESUBMIT"), new AmbiguousOutcomeHandler(client, 3).submit("k"));
        assertEquals(2, client.submitCalls, "at most one resubmission");
    }
}
