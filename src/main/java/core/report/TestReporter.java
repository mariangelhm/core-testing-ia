package core.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import core.errors.ReportException;
import core.log.LoggerUtil;
import core.log.StructuredLog;

/**
 * Utility responsible for generating customizable execution reports.
 */
public final class TestReporter {

    private static final Logger LOGGER = LoggerUtil.getLogger(TestReporter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withLocale(Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    private static Path reportsDirectory = Paths.get("reports");

    private TestReporter() {
        // Utility class
    }

    /**
     * Overrides the directory used to persist execution reports.
     *
     * @param directory directory where reports will be written
     */
    public static void setReportsDirectory(Path directory) {
        reportsDirectory = Objects.requireNonNull(directory, "directory");
    }

    /**
     * Generates a report using the provided customizer to populate metadata and cases.
     *
     * @param reportName logical name of the report
     * @param customizer builder customizer with report content
     * @return path to the generated report file
     */
    public static Path generate(String reportName, Consumer<ReportBuilder> customizer) {
        Objects.requireNonNull(reportName, "reportName");
        Objects.requireNonNull(customizer, "customizer");

        ReportBuilder builder = new ReportBuilder(reportName);
        customizer.accept(builder);

        Map<String, Object> payload = builder.buildPayload();
        Path directory = reportsDirectory;
        String fileName = builder.resolveFileName();
        Path reportFile = directory.resolve(fileName);

        try {
            Files.createDirectories(directory);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(reportFile.toFile(), payload);
        } catch (IOException e) {
            StructuredLog.openAlert(LOGGER, "REPORTE - ERROR AL GENERAR")
                    .line("Reporte", reportName)
                    .line("Archivo", reportFile)
                    .line("Detalle", e.getMessage())
                    .close();
            throw new ReportException("Unable to generate report '" + reportName + "'", e);
        }

        StructuredLog.Block block = StructuredLog.open(LOGGER, "REPORTERÍA", reportName);
        block.line("Archivo", reportFile.toAbsolutePath());
        block.line("Casos", builder.caseCount());
        if (!builder.metadata().isEmpty()) {
            block.line("Metadatos", builder.metadata());
        }
        block.close("Reporte generado");
        return reportFile;
    }

    /**
     * Builder used to customize report metadata and cases.
     */
    public static final class ReportBuilder {

        private final String name;
        private final Map<String, Object> metadata = new LinkedHashMap<>();
        private final Map<String, Object> sections = new LinkedHashMap<>();
        private final List<Map<String, Object>> cases = new ArrayList<>();
        private String description;
        private String fileName;

        private ReportBuilder(String name) {
            this.name = name;
        }

        /**
         * Sets a descriptive summary shown in the report.
         *
         * @param description summary text
         * @return current builder
         */
        public ReportBuilder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Overrides the base file name (without extension) used to persist the report.
         *
         * @param baseName desired base name
         * @return current builder
         */
        public ReportBuilder fileName(String baseName) {
            this.fileName = baseName;
            return this;
        }

        /**
         * Adds a metadata entry.
         *
         * @param key   metadata key
         * @param value metadata value
         * @return current builder
         */
        public ReportBuilder metadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        /**
         * Adds a customizable section to the report root payload.
         *
         * @param name  section name
         * @param value section content
         * @return current builder
         */
        public ReportBuilder section(String name, Object value) {
            sections.put(name, value);
            return this;
        }

        /**
         * Adds a test case entry using the provided consumer.
         *
         * @param consumer builder customizer
         * @return current builder
         */
        public ReportBuilder addTestCase(Consumer<TestCaseBuilder> consumer) {
            TestCaseBuilder builder = new TestCaseBuilder();
            consumer.accept(builder);
            cases.add(builder.build());
            return this;
        }

        private Map<String, Object> buildPayload() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("name", name);
            payload.put("generatedAt", StructuredLog.formatInstant(System.currentTimeMillis()));
            if (description != null && !description.isBlank()) {
                payload.put("description", description);
            }
            if (!metadata.isEmpty()) {
                payload.put("metadata", new LinkedHashMap<>(metadata));
            }
            if (!cases.isEmpty()) {
                payload.put("cases", new ArrayList<>(cases));
            }
            if (!sections.isEmpty()) {
                payload.put("sections", new LinkedHashMap<>(sections));
            }
            payload.put("caseCount", cases.size());
            return payload;
        }

        private String resolveFileName() {
            String base = fileName != null && !fileName.isBlank() ? fileName : name;
            String sanitized = base.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9-_]+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("(^-|-$)", "");
            if (sanitized.isBlank()) {
                sanitized = "reporte";
            }
            String timestamp = FILE_TIMESTAMP.format(Instant.now());
            return sanitized + "-" + timestamp + ".json";
        }

        private int caseCount() {
            return cases.size();
        }

        private Map<String, Object> metadata() {
            return metadata;
        }
    }

    /**
     * Builder used to customize a test case entry.
     */
    public static final class TestCaseBuilder {

        private final Map<String, Object> values = new LinkedHashMap<>();
        private final List<Map<String, Object>> evidences = new ArrayList<>();

        /**
         * Sets the test identifier (e.g. scenario key).
         *
         * @param id identifier value
         * @return current builder
         */
        public TestCaseBuilder id(String id) {
            values.put("id", id);
            return this;
        }

        /**
         * Sets the display name for the case.
         *
         * @param name test name
         * @return current builder
         */
        public TestCaseBuilder name(String name) {
            values.put("name", name);
            return this;
        }

        /**
         * Sets the execution status (e.g. PASSED, FAILED).
         *
         * @param status execution status
         * @return current builder
         */
        public TestCaseBuilder status(String status) {
            values.put("status", status);
            return this;
        }

        /**
         * Records the execution duration.
         *
         * @param duration execution duration
         * @return current builder
         */
        public TestCaseBuilder duration(Duration duration) {
            values.put("durationMillis", duration.toMillis());
            values.put("duration", StructuredLog.formatDuration(duration));
            return this;
        }

        /**
         * Adds a custom attribute to the case payload.
         *
         * @param key   attribute name
         * @param value attribute value
         * @return current builder
         */
        public TestCaseBuilder attribute(String key, Object value) {
            values.put(key, value);
            return this;
        }

        /**
         * Records an evidence entry such as a screenshot or log file.
         *
         * @param description evidence description
         * @param location    evidence path or URL
         * @return current builder
         */
        public TestCaseBuilder addEvidence(String description, String location) {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("description", description);
            evidence.put("location", location);
            evidences.add(evidence);
            return this;
        }

        private Map<String, Object> build() {
            if (!evidences.isEmpty()) {
                values.put("evidences", new ArrayList<>(evidences));
            }
            return new LinkedHashMap<>(values);
        }
    }
}
