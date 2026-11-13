package module10.happensbefore;

import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

class HappensBeforeTest {

    @RepeatedTest(20)
    void everyEdgePublishesThePlainField() throws Exception {
        assertEquals(42, HappensBefore.viaThreadStart());
        assertEquals(7, HappensBefore.viaThreadJoin());
        assertEquals(99, HappensBefore.viaVolatile());
        assertEquals(1234, HappensBefore.viaLatch());
        assertEquals(5, HappensBefore.viaMonitor());
    }
}
