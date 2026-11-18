package module18.ch09;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32C;

/**
 * Item 66 - Use native methods judiciously.
 *
 * <p>JNI code is unsafe (memory corruption crashes the whole JVM), platform-specific, hard to debug, and calls cost
 * more than you think. The JVM is fast enough for almost everything: checksums, crypto ({@code javax.crypto} with
 * hardware intrinsics), compression. Here a settlement-file checksum uses {@link CRC32C} (Java 9, intrinsified by the
 * JIT) instead of a native library. If you must call native code, the Foreign Function and Memory API is the modern
 * route: preview in Java 19-21, final in Java 22 (JEP 454); not used here because this module avoids preview APIs.
 */
public final class Item66NativeMethods {
    private Item66NativeMethods() {
    }

    public static long fileChecksum(String content) {
        CRC32C crc = new CRC32C();
        crc.update(content.getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }
}
