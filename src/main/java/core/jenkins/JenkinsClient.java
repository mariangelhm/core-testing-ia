package core.jenkins;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.errors.JenkinsException;
import core.log.LoggerUtil;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

/**
 * Jenkins REST client offering helpers to trigger jobs and fetch execution data.
 */
public class JenkinsClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final String baseUrl;
    private final String credentials;
    private final ObjectMapper objectMapper;

    /**
     * Creates a client using default HTTP and JSON utilities.
     *
     * @param baseUrl Jenkins base URL (e.g. {@code https://jenkins.company.com})
     * @param username Jenkins username
     * @param token API token or password
     */
    public JenkinsClient(String baseUrl, String username, String token) {
        this(baseUrl, username, token, new OkHttpClient(), new ObjectMapper());
    }

    /**
     * Creates a client providing custom HTTP and JSON helpers.
     *
     * @param baseUrl Jenkins base URL (e.g. {@code https://jenkins.company.com})
     * @param username Jenkins username
     * @param token API token or password
     * @param client OkHttp client to reuse
     * @param objectMapper object mapper to reuse
     */
    public JenkinsClient(String baseUrl, String username, String token, OkHttpClient client, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.client = client;
        this.credentials = Credentials.basic(username, token, StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    /**
     * Triggers a parameterised Jenkins job invoking {@code buildWithParameters}.
     *
     * @param jobName Jenkins job name
     * @param params query parameters sent to the build endpoint
     */
    public void triggerJob(String jobName, Map<String, String> params) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + "/job/" + jobName + "/buildWithParameters").newBuilder();
        if (params != null) {
            params.forEach(urlBuilder::addQueryParameter);
        }
        RequestBody body = new RequestBody() {
            @Override
            public MediaType contentType() {
                return null;
            }

            @Override
            public void writeTo(BufferedSink sink) {
                // No body required
            }
        };
        Request request = new Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", credentials)
            .post(body)
            .build();
        execute(request, "Unable to trigger Jenkins job");
    }

    /**
     * Retrieves the job execution details as JSON.
     *
     * @param jobName Jenkins job name
     * @param buildId build identifier
     * @return JSON payload returned by Jenkins
     */
    public String getJobStatus(String jobName, int buildId) {
        HttpUrl url = HttpUrl.parse(baseUrl + "/job/" + jobName + "/" + buildId + "/api/json");
        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", credentials)
            .get()
            .build();
        return execute(request, "Unable to fetch job status");
    }

    /**
     * Retrieves the progressive text logs for a Jenkins build.
     *
     * @param jobName Jenkins job name
     * @param buildId build identifier
     * @return raw logs for the build
     */
    public String getJobLogs(String jobName, int buildId) {
        HttpUrl url = HttpUrl.parse(baseUrl + "/job/" + jobName + "/" + buildId + "/logText/progressiveText");
        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", credentials)
            .get()
            .build();
        return execute(request, "Unable to fetch job logs");
    }

    /**
     * Triggers a Jenkins job sending a JSON body payload.
     *
     * @param jobName Jenkins job name
     * @param body request body serialised to JSON
     */
    public void triggerJsonJob(String jobName, Map<String, Object> body) {
        HttpUrl url = HttpUrl.parse(baseUrl + "/job/" + jobName + "/build");
        RequestBody requestBody = createJsonBody(body);
        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", credentials)
            .post(requestBody)
            .build();
        execute(request, "Unable to trigger Jenkins job with JSON body");
    }

    private RequestBody createJsonBody(Map<String, Object> body) {
        String payload = serialize(body);
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return JSON;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.writeString(payload, StandardCharsets.UTF_8);
            }
        };
    }

    private String serialize(Map<String, Object> body) {
        try {
            return body == null ? "{}" : objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new JenkinsException("Unable to serialize JSON body", e);
        }
    }

    private String execute(Request request, String errorMessage) {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                LoggerUtil.getLogger(JenkinsClient.class)
                    .error("Jenkins API responded with status {} and body {}", response.code(), responseBody);
                throw new JenkinsException(errorMessage + ": " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        } catch (IOException e) {
            throw new JenkinsException(errorMessage, e);
        }
    }
}
