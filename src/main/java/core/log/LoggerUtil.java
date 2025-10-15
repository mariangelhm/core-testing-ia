package core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

/**
 * Utility class centralizing logging features for the QA framework.
 */
public final class LoggerUtil {

    private static final String[] QA_BANNER = {
            "======================================================================================",
            "=                                                                                    =",
            "=                              QA TESTING - LOGS                                     =",
            "=                                                                                    =",
            "======================================================================================" };

    static {
        Logger bannerLogger = LoggerFactory.getLogger("QA TESTING");
        for (String line : QA_BANNER) {
            bannerLogger.info(line);
        }
    }

    private LoggerUtil() {
        // Utility class
    }

    /**
     * Retrieves an SLF4J {@link Logger} for the provided class.
     *
     * @param clazz target class
     * @return logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * Updates the root logging level dynamically.
     *
     * @param levelName new level name, e.g. INFO, DEBUG
     */
    public static void setRootLevel(String levelName) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Level level = Level.toLevel(levelName, Level.INFO);
        context.getLogger("ROOT").setLevel(level);
    }

    /**
     * Adds a custom appender to the logging context, allowing consumers to centralize
     * reporting in tools such as Jira or custom dashboards.
     *
     * @param appender appender instance
     */
    public static void addAppender(Appender<ILoggingEvent> appender) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        appender.setContext(context);
        appender.start();
        context.getLogger("ROOT").addAppender(appender);
    }
}
