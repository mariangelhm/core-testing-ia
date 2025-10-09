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
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.errors.JiraException;
import core.log.LoggerUtil;

/**
 * Minimal Jira/Xray REST client supporting the typical operations required by automation.
 */
public class JiraClient {

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String authHeader;

    public JiraClient(String baseUrl, String username, String token) {
        this(baseUrl, username, token, HttpClient.newBuilder().connectTimeout(DEFAULT_TIMEOUT).build(), new ObjectMapper());
    }

    public JiraClient(String baseUrl, String username, String token, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        String credentials = username + ":" + token;
        this.authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    public String createTest(String projectKey, String gherkinScenario) {
        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("summary", "Automated test for " + projectKey);
            fields.put("project", Map.of("key", projectKey));
            fields.put("issuetype", Map.of("name", "Test"));
            fields.put("description", gherkinScenario);

            Map<String, Object> payload = Map.of("fields", fields);
            HttpResponse<String> response = sendPost("/rest/api/2/issue", payload);
            ensureSuccess(response, "Unable to create Jira Test");
            JsonNode node = objectMapper.readTree(response.body());
            return node.path("key").asText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JiraException("Error creating Jira Test", e);
        } catch (IOException e) {
            throw new JiraException("Error creating Jira Test", e);
        }
    }

    public void addTestToExecution(String executionKey, String testKey) {
        Map<String, Object> payload = Map.of(
            "addTests", new String[] { testKey }
        );
        try {
            HttpResponse<String> response = sendPost(String.format("/rest/raven/1.0/api/testexec/%s/test", urlEncode(executionKey)), payload);
            ensureSuccess(response, "Unable to add test to execution");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JiraException("Error adding test to execution", e);
        } catch (IOException e) {
            throw new JiraException("Error adding test to execution", e);
        }
    }

    public void reportResult(String executionKey, String testKey, String status) {
        Map<String, Object> payload = Map.of(
            "testExecutionKey", executionKey,
            "test", Map.of("key", testKey),
            "status", status
        );
        try {
            HttpResponse<String> response = sendPost("/rest/raven/1.0/api/import/execution", payload);
            ensureSuccess(response, "Unable to report execution result");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JiraException("Error reporting test result", e);
        } catch (IOException e) {
            throw new JiraException("Error reporting test result", e);
        }
    }

    public JsonNode getExecutionResults(String executionKey) {
        try {
            HttpRequest request = baseRequest(String.format("/rest/raven/1.0/api/testexec/%s/test", urlEncode(executionKey)))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "Unable to retrieve execution results");
            return objectMapper.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JiraException("Error retrieving execution results", e);
        } catch (IOException e) {
            throw new JiraException("Error retrieving execution results", e);
        }
    }

    private HttpResponse<String> sendPost(String path, Object payload) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest request = baseRequest(path)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", CONTENT_TYPE_JSON)
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder()
            .timeout(DEFAULT_TIMEOUT)
            .uri(URI.create(baseUrl + path))
            .header("Authorization", authHeader)
            .header("Accept", CONTENT_TYPE_JSON)
            .header("User-Agent", "qa-core/1.0");
    }

    private void ensureSuccess(HttpResponse<String> response, String errorMessage) {
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            LoggerUtil.getLogger(JiraClient.class).error("Jira API responded with status {} and body {}", statusCode, response.body());
            throw new JiraException(errorMessage + ": " + statusCode);
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
