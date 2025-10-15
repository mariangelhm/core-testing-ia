package core.jira;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.errors.JiraException;
import core.log.LoggerUtil;
import core.log.StructuredLog;
import org.slf4j.Logger;

/**
 * Minimal Jira/Xray REST client supporting the typical operations required by automation.
 */
public class JiraClient {

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Logger LOGGER = LoggerUtil.getLogger(JiraClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String authHeader;

    /**
     * Creates a client using default HTTP and JSON utilities.
     *
     * @param baseUrl Jira base URL (e.g. {@code https://jira.company.com})
     * @param username Jira username
     * @param token API token or password
     */
    public JiraClient(String baseUrl, String username, String token) {
        this(baseUrl, username, token, HttpClient.newBuilder().connectTimeout(DEFAULT_TIMEOUT).build(), new ObjectMapper());
    }

    /**
     * Creates a client providing custom HTTP and JSON helpers.
     *
     * @param baseUrl Jira base URL (e.g. {@code https://jira.company.com})
     * @param username Jira username
     * @param token API token or password
     * @param httpClient HTTP client to reuse
     * @param objectMapper object mapper to reuse
     */
    public JiraClient(String baseUrl, String username, String token, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        String credentials = username + ":" + token;
        this.authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates a Jira test issue using the provided scenario description.
     *
     * @param projectKey Jira project key
     * @param gherkinScenario description used as issue body
     * @return created test issue key
     */
    public String createTest(String projectKey, String gherkinScenario) {
        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("summary", "Automated test for " + projectKey);
            fields.put("project", Map.of("key", projectKey));
            fields.put("issuetype", Map.of("name", "Test"));
            fields.put("description", gherkinScenario);

            Map<String, Object> payload = Map.of("fields", fields);
            HttpResponse<String> response = sendPost("/rest/api/2/issue", payload, "Unable to create Jira Test");
            JsonNode node = objectMapper.readTree(response.body());
            return node.path("key").asText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JiraException("Error creating Jira Test", e);
        } catch (IOException e) {
            throw new JiraException("Error creating Jira Test", e);
        }
    }

    /**
     * Adds an existing test to the given execution key.
     *
     * @param executionKey Xray execution key
     * @param testKey Jira test issue key
     */
    public void addTestToExecution(String executionKey, String testKey) {
        Map<String, Object> payload = Map.of(
            "addTests", new String[] { testKey }
        );
        try {
            sendPost(String.format("/rest/raven/1.0/api/testexec/%s/test", urlEncode(executionKey)), payload,
                    "Unable to add test to execution");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JiraException("Error adding test to execution", e);
        } catch (IOException e) {
            throw new JiraException("Error adding test to execution", e);
        }
    }

    /**
     * Reports the execution status for a test inside an execution.
     *
     * @param executionKey Xray execution key
     * @param testKey Jira test issue key
     * @param status execution status (e.g. {@code PASS}, {@code FAIL})
     */
    public void reportResult(String executionKey, String testKey, String status) {
        Map<String, Object> payload = Map.of(
            "testExecutionKey", executionKey,
            "test", Map.of("key", testKey),
            "status", status
        );
        try {
            sendPost("/rest/raven/1.0/api/import/execution", payload, "Unable to report execution result");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JiraException("Error reporting test result", e);
        } catch (IOException e) {
            throw new JiraException("Error reporting test result", e);
        }
    }

    /**
     * Retrieves the execution results for the provided execution key.
     *
     * @param executionKey Xray execution key
     * @return execution results payload as {@link JsonNode}
     */
    public JsonNode getExecutionResults(String executionKey) {
        try {
            String path = String.format("/rest/raven/1.0/api/testexec/%s/test", urlEncode(executionKey));
            HttpRequest request = baseRequest(path)
                .GET()
                .build();
            HttpResponse<String> response = send(request, "GET " + path, "Unable to retrieve execution results", null);
            return objectMapper.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JiraException("Error retrieving execution results", e);
        } catch (IOException e) {
            throw new JiraException("Error retrieving execution results", e);
        }
    }

    private HttpResponse<String> sendPost(String path, Object payload, String errorMessage)
            throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest request = baseRequest(path)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", CONTENT_TYPE_JSON)
            .build();
        return send(request, "POST " + path, errorMessage, body);
    }

    private HttpResponse<String> send(HttpRequest request, String summary, String errorMessage, String requestBody)
            throws IOException, InterruptedException {
        StructuredLog.Block block = StructuredLog.open(LOGGER, "JIRA", summary);
        block.line("URL", request.uri());
        if (requestBody != null && !requestBody.isBlank()) {
            block.line("Body", requestBody);
        }
        long start = System.nanoTime();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Duration duration = Duration.ofNanos(System.nanoTime() - start);
            block.section("RESPUESTA");
            block.line("Status", response.statusCode());
            block.line("Body", response.body());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (!success) {
                block.section("ERROR");
                block.error("Motivo", errorMessage);
            }
            block.close(String.format(Locale.ROOT, "%s | %s", summary, StructuredLog.formatDuration(duration)));
            if (!success) {
                StructuredLog.openAlert(LOGGER, "JIRA - RESPUESTA NO EXITOSA")
                        .line("Solicitud", summary)
                        .line("Status", response.statusCode())
                        .line("Body", StructuredLog.abbreviate(response.body()))
                        .close();
                throw new JiraException(errorMessage + ": " + response.statusCode());
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Duration duration = Duration.ofNanos(System.nanoTime() - start);
            block.section("ERROR");
            block.error("Interrupci\u00F3n", e.getMessage());
            block.close(String.format(Locale.ROOT, "%s | interrumpido tras %s", summary,
                    StructuredLog.formatDuration(duration)));
            StructuredLog.openAlert(LOGGER, errorMessage)
                    .line("Solicitud", summary)
                    .line("Detalle", e.getMessage())
                    .close();
            throw e;
        } catch (IOException e) {
            Duration duration = Duration.ofNanos(System.nanoTime() - start);
            block.section("ERROR");
            block.error("Excepci\u00F3n", e.getMessage());
            block.close(String.format(Locale.ROOT, "%s | error tras %s", summary,
                    StructuredLog.formatDuration(duration)));
            StructuredLog.openAlert(LOGGER, errorMessage)
                    .line("Solicitud", summary)
                    .line("Detalle", e.getMessage())
                    .close();
            throw e;
        }
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder()
            .timeout(DEFAULT_TIMEOUT)
            .uri(URI.create(baseUrl + path))
            .header("Authorization", authHeader)
            .header("Accept", CONTENT_TYPE_JSON)
            .header("User-Agent", "qa-core/1.0");
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
