package module12.executors;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class AsyncExecutorsTest {

    @Test
    void stagesRunWhereYouTellThem() throws Exception {
        ExecutorService io = AsyncExecutors.ioPool("io", 2);
        ExecutorService cpu = AsyncExecutors.ioPool("cpu", 2);
        try {
            String[] names = AsyncExecutors.whoRunsWhat(io, cpu).join();
            assertTrue(names[0].startsWith("io-"), names[0]);
            assertTrue(names[1].startsWith("io-") || names[1].equals(Thread.currentThread().getName()), "thenApply runs on completing thread or caller: " + names[1]);
            assertTrue(names[2].startsWith("cpu-"), names[2]);
            assertTrue(AsyncExecutors.blockingOnIoPool(5, io).join().startsWith("io-"));
            assertTrue(AsyncExecutors.blockingInCommonPoolBad(1).join().contains("ForkJoinPool") || Runtime.getRuntime().availableProcessors() <= 2);
        } finally {
            io.shutdownNow();
            cpu.shutdownNow();
        }
    }
}
