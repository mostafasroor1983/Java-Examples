package module14.classloading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassLoadingDemoTest {

    @Test
    void initialisationIsLazyAndConstantsDoNotTriggerIt() throws Exception {
        ClassLoadingDemo.INIT_LOG.clear();
        Class<?> loaded = ClassLoadingDemo.loadWithoutInit(ClassLoadingDemo.FeeTable.class.getName());
        assertNotNull(loaded);
        assertTrue(ClassLoadingDemo.INIT_LOG.isEmpty(), "loading must not initialise");
        assertEquals("v1", ClassLoadingDemo.FeeTable.VERSION);
        assertTrue(ClassLoadingDemo.INIT_LOG.isEmpty(), "compile-time constant is inlined, no init");
        assertTrue(ClassLoadingDemo.FeeTable.LOADED_AT > 0);
        assertEquals(1, ClassLoadingDemo.INIT_LOG.size(), "first active use initialises exactly once");
        ClassLoadingDemo.FeeTable.rate("CARD");
        assertEquals(1, ClassLoadingDemo.INIT_LOG.size());
    }

    @Test
    void loaderHierarchy() throws Exception {
        assertEquals("bootstrap", ClassLoadingDemo.loaderChain(String.class));
        String chain = ClassLoadingDemo.loaderChain(ClassLoadingDemo.class);
        assertTrue(chain.startsWith("app"), chain);
        assertTrue(chain.endsWith("bootstrap"), chain);
        assertTrue(ClassLoadingDemo.sameClassFromTwoLoaders(), "parent-first delegation yields one Class object");
    }
}
