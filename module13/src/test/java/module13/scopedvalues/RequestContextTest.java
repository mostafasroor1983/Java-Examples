package module13.scopedvalues;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class RequestContextTest {

    @Test
    void boundOnlyInsideScope() throws Exception {
        assertFalse(RequestContext.isBound());
        assertThrows(NoSuchElementException.class, RequestContext::current);
        String traceId = RequestContext.runWith(new RequestContext.Context("t1", "c1"), () -> RequestContext.current().traceId());
        assertEquals("t1", traceId);
        assertFalse(RequestContext.isBound(), "unbound automatically after the call");
    }

    @Test
    void nestedRebindingAndInheritance() throws Exception {
        assertEquals("outer/inner/outer", RequestContext.nestedRebinding());
        assertEquals("trace-42|true:trace-42", RequestContext.traceIdSeenBySubtasks());
    }

    @Test
    void threadLocalContrast() throws Exception {
        assertNull(RequestContext.ThreadLocalContext.current());
        String id = RequestContext.ThreadLocalContext.runWith(new RequestContext.Context("tl", "c"), () -> RequestContext.ThreadLocalContext.current().traceId());
        assertEquals("tl", id);
        assertNull(RequestContext.ThreadLocalContext.current(), "remove() in finally prevents the leak");
    }
}
