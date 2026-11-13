package module09.sync;

/**
 * The correct use of volatile: a flag written by one thread and read by others.
 * Without volatile the JIT may hoist the read out of the loop and the worker never stops.
 */
public final class VolatileFlag {
    private volatile boolean running = true;
    private long iterations;

    public void stop() {
        running = false;
    }

    /** Spins until stop(); returns iterations. The volatile read happens every iteration. */
    public long runUntilStopped() {
        while (running) {
            iterations++;
        }
        return iterations;
    }
}
