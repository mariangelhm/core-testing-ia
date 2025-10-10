package core.utils;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Helper for generating deterministic unique identifiers.
 */
public final class UniqueIdGenerator {

    private static final AtomicLong COUNTER = new AtomicLong();

    private UniqueIdGenerator() {
    }

    /**
     * Generates a random UUID string.
     *
     * @return new UUID value
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates an identifier using the current epoch milli and an incremental counter.
     *
     * @param prefix prefix added to the generated identifier
     * @return unique identifier composed by the prefix, timestamp and counter
     */
    public static String timestamped(String prefix) {
        long value = COUNTER.incrementAndGet();
        return prefix + "-" + Instant.now().toEpochMilli() + "-" + value;
    }
}
