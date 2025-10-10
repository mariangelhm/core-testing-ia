package core.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Date helper functions for test automation.
 */
public final class DateUtil {

    private DateUtil() {
    }

    /**
     * Returns the current timestamp formatted using ISO-8601.
     *
     * @return ISO-8601 formatted timestamp
     */
    public static String nowIso() {
        return Instant.now().toString();
    }

    /**
     * Formats the provided date using the supplied pattern.
     *
     * @param dateTime date to format
     * @param pattern formatter pattern compatible with {@link DateTimeFormatter}
     * @return formatted date string
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Parses the provided value using the supplied pattern.
     *
     * @param value text to parse
     * @param pattern formatter pattern compatible with {@link DateTimeFormatter}
     * @return parsed {@link LocalDateTime}
     */
    public static LocalDateTime parse(String value, String pattern) {
        return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Converts epoch milliseconds to {@link LocalDateTime} within the provided zone.
     *
     * @param epochMillis epoch milliseconds to convert
     * @param zoneId timezone used for the conversion
     * @return resulting {@link LocalDateTime}
     */
    public static LocalDateTime fromEpochMillis(long epochMillis, ZoneId zoneId) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zoneId);
    }
}
