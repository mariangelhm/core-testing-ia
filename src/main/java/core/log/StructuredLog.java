package core.log;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;

/**
 * Helper responsible for rendering highly legible log blocks that share the same
 * visual style across services, database operations and integrations.
 */
public final class StructuredLog {

    private static final int BOX_WIDTH = 90;
    private static final int LABEL_WIDTH = 22;
    private static final int PREVIEW_LIMIT = 180;
    private static final String TOP = edge('=');
    private static final String DIVIDER = edge('-');

    private StructuredLog() {
        // Utility class
    }

    private static String edge(char fill) {
        return "+" + repeat(fill, BOX_WIDTH - 2) + "+";
    }

    private static String repeat(char value, int times) {
        return String.valueOf(value).repeat(Math.max(0, times));
    }

    private static String renderBanner(String title, char fill) {
        String normalized = " " + title.toUpperCase(Locale.ROOT).trim() + " ";
        int innerWidth = BOX_WIDTH - 2;
        if (normalized.length() > innerWidth) {
            normalized = normalized.substring(0, innerWidth);
        }
        int remaining = innerWidth - normalized.length();
        int left = remaining / 2;
        int right = remaining - left;
        return "|" + repeat(fill, left) + normalized + repeat(fill, right) + "|";
    }

    private static String renderLine(String label, Object value) {
        String singleLine = abbreviate(value);
        String base = String.format(Locale.ROOT, " %1$-" + LABEL_WIDTH + "s : %2$s", label, singleLine);
        int innerWidth = BOX_WIDTH - 2;
        if (base.length() > innerWidth) {
            base = base.substring(0, innerWidth - 3) + "...";
        }
        if (base.length() < innerWidth) {
            base = base + repeat(' ', innerWidth - base.length());
        }
        return "|" + base + "|";
    }

    /**
     * Opens a structured info block that can be used to log start/end markers with
     * consistent formatting.
     *
     * @param logger  target logger
     * @param context logical name of the block (e.g. SERVICIO, BASE DE DATOS)
     * @param summary brief summary displayed on the opening line
     * @return opened block ready to log additional lines
     */
    public static Block open(Logger logger, String context, String summary) {
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(context, "context");
        logger.info(TOP);
        logger.info(renderLine("INICIO " + context, summary));
        logger.info(DIVIDER);
        return new Block(logger, context);
    }

    /**
     * Opens an error alert block rendered with the same typography as information
     * blocks but emphasised for quick scanning.
     *
     * @param logger target logger
     * @param title  alert title shown in the banner
     * @return alert builder to log additional lines
     */
    public static Alert openAlert(Logger logger, String title) {
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(title, "title");
        logger.error(TOP);
        logger.error(renderBanner(title, '!'));
        logger.error(DIVIDER);
        return new Alert(logger);
    }

    /**
     * Renders a key/value pair into the log block using the shared style.
     *
     * @param label property name
     * @param value property value
     * @return formatted line ready to be logged
     */
    public static String line(String label, Object value) {
        return renderLine(label, value);
    }

    /**
     * Collapses text into a single line removing blank characters.
     *
     * @param value value to format
     * @return single-line representation
     */
    public static String toSingleLine(Object value) {
        if (value == null) {
            return "<null>";
        }
        String text = String.valueOf(value);
        String collapsed = text.replaceAll("\\s*\\r?\\n\\s*", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        return collapsed.isEmpty() ? "<vacío>" : collapsed;
    }

    /**
     * Collapses text into a single line and truncates it to keep logs concise.
     *
     * @param value value to format
     * @return abbreviated representation
     */
    public static String abbreviate(Object value) {
        String single = toSingleLine(value);
        if (single.length() <= PREVIEW_LIMIT) {
            return single;
        }
        return single.substring(0, PREVIEW_LIMIT - 3) + "...";
    }

    /**
     * Formats a {@link Duration} in milliseconds for readability.
     *
     * @param duration duration to format
     * @return human readable text
     */
    public static String formatDuration(Duration duration) {
        Duration truncated = duration.truncatedTo(ChronoUnit.MILLIS);
        return truncated.toMillis() + " ms";
    }

    /**
     * Builder for informational blocks.
     */
    public static final class Block {

        private final Logger logger;
        private final String context;

        private Block(Logger logger, String context) {
            this.logger = logger;
            this.context = context;
        }

        /**
         * Logs a key/value line at info level.
         *
         * @param label property name
         * @param value property value
         * @return current block
         */
        public Block line(String label, Object value) {
            logger.info(renderLine(label, value));
            return this;
        }

        /**
         * Logs a key/value line at warn level.
         *
         * @param label property name
         * @param value property value
         * @return current block
         */
        public Block warn(String label, Object value) {
            logger.warn(renderLine(label, value));
            return this;
        }

        /**
         * Logs a key/value line at error level.
         *
         * @param label property name
         * @param value property value
         * @return current block
         */
        public Block error(String label, Object value) {
            logger.error(renderLine(label, value));
            return this;
        }

        /**
         * Inserts a visual divider and optional title inside the block.
         *
         * @param title section title
         * @return current block
         */
        public Block section(String title) {
            logger.info(DIVIDER);
            if (title != null && !title.isBlank()) {
                logger.info(renderBanner(title, '-'));
                logger.info(DIVIDER);
            }
            return this;
        }

        /**
         * Closes the block logging a final summary line.
         *
         * @param summary text displayed alongside the closing marker
         */
        public void close(String summary) {
            logger.info(DIVIDER);
            logger.info(renderLine("FIN " + context, summary));
            logger.info(TOP);
        }
    }

    /**
     * Builder for error alert blocks.
     */
    public static final class Alert {

        private final Logger logger;
        private boolean needsDivider;

        private Alert(Logger logger) {
            this.logger = logger;
        }

        /**
         * Logs a key/value line inside the alert.
         *
         * @param label property name
         * @param value property value
         * @return current alert
         */
        public Alert line(String label, Object value) {
            logger.error(renderLine(label, value));
            needsDivider = true;
            return this;
        }

        /**
         * Inserts a titled section within the alert block.
         *
         * @param title section title
         * @return current alert
         */
        public Alert section(String title) {
            if (needsDivider) {
                logger.error(DIVIDER);
            }
            logger.error(renderBanner(title, '!'));
            logger.error(DIVIDER);
            needsDivider = false;
            return this;
        }

        /**
         * Appends multiple lines based on the provided key/value map.
         *
         * @param entries data to render
         * @return current alert
         */
        public Alert lines(Map<String, ?> entries) {
            if (entries != null) {
                for (Map.Entry<String, ?> entry : entries.entrySet()) {
                    line(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        /**
         * Finalises the alert printing the closing border.
         */
        public void close() {
            logger.error(TOP);
        }
    }

    /**
     * Creates a defensive copy of the provided map collapsing values into
     * single-line representations.
     *
     * @param source map to normalise
     * @return normalised map
     */
    public static Map<String, String> normalise(Map<String, ?> source) {
        Map<String, String> normalised = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, ?> entry : source.entrySet()) {
                normalised.put(entry.getKey(), abbreviate(entry.getValue()));
            }
        }
        return normalised;
    }

    /**
     * Formats an instant using ISO-8601 down to seconds to keep reports concise.
     *
     * @param instantMillis epoch milliseconds
     * @return formatted string
     */
    public static String formatInstant(long instantMillis) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(java.time.Instant.ofEpochMilli(instantMillis).atOffset(java.time.ZoneOffset.UTC));
    }
}
