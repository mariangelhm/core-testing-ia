package core.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads configuration from property files, environment variables and JVM parameters.
 */
public final class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final String DEFAULT_ENVIRONMENT = "default";
    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String APPLICATION_PROPERTIES_PATTERN = "application-%s.properties";

    private static final Properties PROPERTIES = new Properties();
    private static final ConcurrentMap<String, String> CACHE = new ConcurrentHashMap<>();
    private static volatile boolean INITIALIZED = false;

    private ConfigManager() {
        // Utility class
    }

    /**
     * Initializes the configuration loading all the sources in the proper order.
     *
     * @param environment the environment to load, optional. If null the environment
     *                    will be resolved using {@code qa.env}, {@code ENV} or defaults.
     */
    public static synchronized void initialize(String environment) {
        if (INITIALIZED) {
            return;
        }
        PROPERTIES.clear();

        String resolvedEnv = Optional.ofNullable(environment)
            .orElseGet(ConfigManager::resolveEnvironment);

        loadFromClasspath(APPLICATION_PROPERTIES);
        if (!Objects.equals(resolvedEnv, DEFAULT_ENVIRONMENT)) {
            loadFromClasspath(String.format(Locale.ROOT, APPLICATION_PROPERTIES_PATTERN, resolvedEnv));
            loadFromWorkingDirectory(String.format(Locale.ROOT, APPLICATION_PROPERTIES_PATTERN, resolvedEnv));
        }
        loadFromWorkingDirectory(APPLICATION_PROPERTIES);

        System.getenv().forEach((key, value) -> PROPERTIES.setProperty(key, value));
        System.getProperties().forEach((key, value) -> PROPERTIES.setProperty(String.valueOf(key), String.valueOf(value)));

        INITIALIZED = true;
        LOGGER.info("Configuration initialized for environment: {}", resolvedEnv);
    }

    private static void loadFromClasspath(String fileName) {
        try (InputStream stream = ConfigManager.class.getClassLoader().getResourceAsStream(fileName)) {
            if (stream != null) {
                Properties props = new Properties();
                props.load(stream);
                PROPERTIES.putAll(props);
                LOGGER.debug("Loaded configuration from classpath file: {}", fileName);
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to load configuration from classpath file {}: {}", fileName, e.getMessage());
        }
    }

    private static void loadFromWorkingDirectory(String fileName) {
        Path path = Path.of(fileName);
        if (!Files.exists(path)) {
            return;
        }
        try (InputStream input = Files.newInputStream(path)) {
            Properties props = new Properties();
            props.load(input);
            PROPERTIES.putAll(props);
            LOGGER.debug("Loaded configuration from file system: {}", path.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.warn("Unable to load configuration from {}: {}", path.toAbsolutePath(), e.getMessage());
        }
    }

    /**
     * Returns the configuration value for the provided key.
     *
     * @param key configuration key
     * @return the value if found, otherwise {@code null}
     */
    public static String get(String key) {
        ensureInitialized();
        return CACHE.computeIfAbsent(key, k -> PROPERTIES.getProperty(k));
    }

    /**
     * Returns the configuration value or the provided default.
     *
     * @param key configuration key
     * @param defaultValue value to return when not present
     * @return config value or default
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid integer for key " + key + " -> " + value, e);
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Retrieves a required value throwing {@link IllegalStateException} if missing.
     *
     * @param key configuration key
     * @return the configuration value
     */
    public static String getRequired(String key) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration key: " + key);
        }
        return value;
    }

    /**
     * Clears the cache and allows reloading the configuration. Intended for tests.
     */
    public static synchronized void reset() {
        PROPERTIES.clear();
        CACHE.clear();
        INITIALIZED = false;
    }

    private static String resolveEnvironment() {
        String fromSystem = System.getProperty("qa.env");
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem.trim();
        }
        String fromEnv = System.getenv("ENV");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return DEFAULT_ENVIRONMENT;
    }

    private static void ensureInitialized() {
        if (!INITIALIZED) {
            initialize(null);
        }
    }
}
