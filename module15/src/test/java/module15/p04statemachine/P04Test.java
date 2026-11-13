package module15.p04statemachine;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static module15.p04statemachine.P04PaymentStateMachine.Event.*;
import static module15.p04statemachine.P04PaymentStateMachine.State.*;
import static org.junit.jupiter.api.Assertions.*;

class P04Test {
    @Test
    void happyPathAndRefund() {
        var sm = new P04PaymentStateMachine();
        assertEquals(AUTHORISED, sm.apply(AUTHORISE));
        assertEquals(CAPTURED, sm.apply(CAPTURE));
        assertEquals(SETTLED, sm.apply(SETTLE));
        assertFalse(sm.isFinal());
        assertEquals(REFUNDED, sm.apply(REFUND));
        assertTrue(sm.isFinal());
    }

    @Test
    void illegalTransitionsAreRejectedAndStateUnchanged() {
        var sm = new P04PaymentStateMachine();
        assertThrows(P04PaymentStateMachine.IllegalTransitionException.class, () -> sm.apply(CAPTURE));
        assertEquals(CREATED, sm.state());
        sm.apply(AUTHORISE);
        sm.apply(VOID);
        assertThrows(P04PaymentStateMachine.IllegalTransitionException.class, () -> sm.apply(CAPTURE));
        assertEquals(EnumSet.of(CAPTURE, VOID, FAIL), P04PaymentStateMachine.allowedFrom(AUTHORISED));
        assertTrue(P04PaymentStateMachine.allowedFrom(REFUNDED).isEmpty());
    }
}
