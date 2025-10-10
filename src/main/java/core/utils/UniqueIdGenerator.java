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

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String timestamped(String prefix) {
        long value = COUNTER.incrementAndGet();
        return prefix + "-" + Instant.now().toEpochMilli() + "-" + value;
    }
}
