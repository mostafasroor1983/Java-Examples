package module14.classloading;

import java.util.ArrayList;
import java.util.List;

/**
 * Class loading: bootstrap -> platform -> application loader, parent-first delegation.
 * Loading != initialisation: static initialisers run on first active use (new, static method/field access, Class.forName).
 */
public final class ClassLoadingDemo {
    private ClassLoadingDemo() {
    }

    public static final List<String> INIT_LOG = new ArrayList<>();

    /** Static init runs once, on first active use, thread-safely (the JVM holds the class init lock). */
    public static final class FeeTable {
        static {
            INIT_LOG.add("FeeTable initialised");
        }

        public static final String VERSION = "v1"; // compile-time constant: reading it does NOT trigger init
        public static final long LOADED_AT = System.nanoTime(); // not a constant: reading it triggers init

        public static String rate(String type) {
            return "CARD".equals(type) ? "1.5%" : "0.20";
        }
    }

    public static String loaderChain(Class<?> c) {
        StringBuilder sb = new StringBuilder();
        ClassLoader l = c.getClassLoader();
        if (l == null) return "bootstrap";
        while (l != null) {
            sb.append(l.getName() == null ? l.getClass().getSimpleName() : l.getName());
            l = l.getParent();
            if (l != null) sb.append(" -> ");
        }
        return sb.append(" -> bootstrap").toString();
    }

    /** Class.forName(name, initialize=false) loads without initialising. */
    public static Class<?> loadWithoutInit(String name) throws ClassNotFoundException {
        return Class.forName(name, false, ClassLoadingDemo.class.getClassLoader());
    }

    /** Two loaders => two distinct Class objects even for the same bytes; classic "ClassCastException: X cannot be cast to X". */
    public static boolean sameClassFromTwoLoaders() throws Exception {
        ClassLoader parent = ClassLoadingDemo.class.getClassLoader();
        // Child loaders that delegate to the same parent see the same class (parent-first).
        ClassLoader a = new ClassLoader(parent) { };
        ClassLoader b = new ClassLoader(parent) { };
        return Class.forName(FeeTable.class.getName(), false, a) == Class.forName(FeeTable.class.getName(), false, b);
    }
}
