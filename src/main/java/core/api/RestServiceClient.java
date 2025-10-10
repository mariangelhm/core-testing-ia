package core.api;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import core.db.DBHelper;
import core.log.LoggerUtil;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Fluent client that centralizes REST interactions and validations for QA services.
 */
public class RestServiceClient {

    private static final Logger LOGGER = LoggerUtil.getLogger(RestServiceClient.class);

    private final RequestSpecBuilder specBuilder = new RequestSpecBuilder();
    private Method method = Method.GET;
    private String url;
    private boolean followRedirects = true;
    private Response response;
    private DBHelper dbHelper;

    /**
     * Assigns the base URL for the request.
     *
     * @param url target endpoint
     * @return client instance for chaining
     */
    public RestServiceClient url(String url) {
        this.url = Objects.requireNonNull(url, "url must not be null");
        return this;
    }

    /**
     * Configures the HTTP method using RestAssured's enum.
     *
     * @param method HTTP method to use
     * @return client instance for chaining
     */
    public RestServiceClient method(Method method) {
        this.method = Objects.requireNonNull(method, "method must not be null");
        return this;
    }

    /**
     * Configures the HTTP method from a string (e.g. GET, POST).
     *
     * @param methodName textual method representation
     * @return client instance for chaining
     */
    public RestServiceClient method(String methodName) {
        Objects.requireNonNull(methodName, "methodName must not be null");
        this.method = Method.valueOf(methodName.trim().toUpperCase(Locale.ROOT));
        return this;
    }

    /**
     * Adds a header to the request.
     *
     * @param name  header name
     * @param value header value
     * @return client instance for chaining
     */
    public RestServiceClient addHeader(String name, Object value) {
        specBuilder.addHeader(name, value);
        return this;
    }

    /**
     * Adds a query parameter to the request.
     *
     * @param name  parameter name
     * @param value parameter value
     * @return client instance for chaining
     */
    public RestServiceClient addQueryParam(String name, Object value) {
        specBuilder.addQueryParam(name, value);
        return this;
    }

    /**
     * Adds a path parameter to the request.
     *
     * @param name  parameter name
     * @param value parameter value
     * @return client instance for chaining
     */
    public RestServiceClient addPathParam(String name, Object value) {
        specBuilder.addPathParam(name, value);
        return this;
    }

    /**
     * Defines the request content type.
     *
     * @param contentType request content type
     * @return client instance for chaining
     */
    public RestServiceClient contentType(ContentType contentType) {
        specBuilder.setContentType(contentType);
        return this;
    }

    /**
     * Sends the provided object as JSON payload.
     *
     * @param body request payload
     * @return client instance for chaining
     */
    public RestServiceClient jsonBody(Object body) {
        specBuilder.setContentType(ContentType.JSON);
        specBuilder.setBody(body);
        return this;
    }

    /**
     * Adds form parameters to the request body.
     *
     * @param formParams key-value pairs
     * @return client instance for chaining
     */
    public RestServiceClient formBody(Map<String, ?> formParams) {
        specBuilder.setContentType(ContentType.URLENC);
        specBuilder.addFormParams(formParams);
        return this;
    }

    /**
     * Adds multipart content such as files to the request.
     *
     * @param controlName field name
     * @param file        file to upload
     * @return client instance for chaining
     */
    public RestServiceClient multiPart(String controlName, File file) {
        specBuilder.addMultiPart(controlName, file);
        return this;
    }

    /**
     * Adds multipart content from an input stream.
     *
     * @param controlName field name
     * @param stream      input stream
     * @param fileName    file name to report to the server
     * @param mimeType    payload type
     * @return client instance for chaining
     */
    public RestServiceClient multiPart(String controlName, InputStream stream, String fileName, String mimeType) {
        specBuilder.addMultiPart(controlName, fileName, stream, mimeType);
        return this;
    }

    /**
     * Sends a binary body with the specified content type.
     *
     * @param bytes       binary payload
     * @param contentType MIME type
     * @return client instance for chaining
     */
    public RestServiceClient binaryBody(byte[] bytes, String contentType) {
        specBuilder.setBody(bytes);
        specBuilder.setContentType(contentType);
        return this;
    }

    /**
     * Assigns a raw body without changing the current content type.
     *
     * @param body payload to send
     * @return client instance for chaining
     */
    public RestServiceClient body(Object body) {
        specBuilder.setBody(body);
        return this;
    }

    /**
     * Enables or disables redirect following.
     *
     * @param follow flag indicating whether redirects should be followed
     * @return client instance for chaining
     */
    public RestServiceClient followRedirects(boolean follow) {
        this.followRedirects = follow;
        return this;
    }

    /**
     * Associates a {@link DBHelper} instance to reuse database utilities from the same client.
     *
     * @param helper configured helper
     * @return client instance for chaining
     */
    public RestServiceClient withDBHelper(DBHelper helper) {
        this.dbHelper = helper;
        return this;
    }

    /**
     * Executes the configured request and stores the response for subsequent validations.
     *
     * @return {@link Response} obtained from the remote service
     */
    public Response execute() {
        if (url == null) {
            throw new IllegalStateException("Debe declararse la URL antes de ejecutar la solicitud");
        }
        RequestSpecification request = RestAssured.given(specBuilder.build())
                .config(RestAssuredConfig.config()
                        .redirect(RedirectConfig.redirectConfig().followRedirects(followRedirects)));
        response = request.request(method, url);
        LOGGER.info("Solicitud ejecutada con estado {}", response.getStatusCode());
        return response;
    }

    private Response ensureResponse() {
        if (response == null) {
            throw new IllegalStateException("Debe ejecutar la solicitud antes de realizar validaciones");
        }
        return response;
    }

    /**
     * Validates that the response status code matches the expected value.
     *
     * @param expectedStatus expected HTTP status code
     * @return client instance for chaining
     */
    public RestServiceClient validateStatusCode(int expectedStatus) {
        Response current = ensureResponse();
        int actual = current.getStatusCode();
        if (actual != expectedStatus) {
            throw new AssertionError(String.format("Código de respuesta esperado %d pero fue %d", expectedStatus, actual));
        }
        LOGGER.info("Validación exitosa de código de respuesta {}", expectedStatus);
        return this;
    }

    /**
     * Validates that the response contains the provided fragment.
     *
     * @param expectedText text to assert
     * @return client instance for chaining
     */
    public RestServiceClient validateBodyContains(String expectedText) {
        Response current = ensureResponse();
        String body = current.getBody().asString();
        if (!body.contains(expectedText)) {
            throw new AssertionError("El cuerpo de la respuesta no contiene el texto esperado: " + expectedText);
        }
        LOGGER.info("Validación exitosa del contenido esperado en la respuesta");
        return this;
    }

    /**
     * Validates the value obtained from a JSON path expression.
     *
     * @param jsonPath expression to evaluate
     * @param expected expected value
     * @return client instance for chaining
     */
    public RestServiceClient validateJsonPathEquals(String jsonPath, Object expected) {
        Object actual = extractJsonPath(jsonPath, Object.class);
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError(String.format("Valor esperado en '%s' era '%s' pero fue '%s'", jsonPath, expected, actual));
        }
        LOGGER.info("Validación exitosa para jsonPath {}", jsonPath);
        return this;
    }

    /**
     * Validates the response time against a target duration.
     *
     * @param threshold  threshold duration
     * @param comparator comparator to apply
     * @return client instance for chaining
     */
    public RestServiceClient validateResponseTime(Duration threshold, TimeComparison comparator) {
        Response current = ensureResponse();
        long elapsed = current.timeIn(TimeUnit.MILLISECONDS);
        long target = threshold.toMillis();
        boolean valid;
        switch (comparator) {
            case LESS_THAN:
                valid = elapsed < target;
                break;
            case GREATER_THAN:
                valid = elapsed > target;
                break;
            case EQUAL_TO:
                valid = elapsed == target;
                break;
            default:
                throw new IllegalStateException("Comparador no soportado: " + comparator);
        }
        if (!valid) {
            throw new AssertionError(String.format("Tiempo de respuesta %d ms no cumple con la condición %s %d ms", elapsed,
                    comparator, target));
        }
        LOGGER.info("Validación exitosa del tiempo de respuesta ({} {} ms)", comparator, target);
        return this;
    }

    /**
     * Validates the response payload using a JSON schema.
     *
     * @param schemaPath path to the schema file
     * @return client instance for chaining
     */
    public RestServiceClient validateJsonSchema(Path schemaPath) {
        ensureResponse().then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(schemaPath.toFile()));
        LOGGER.info("Validación de esquema JSON exitosa usando {}", schemaPath);
        return this;
    }

    /**
     * Extracts a value from the JSON response using the provided path.
     *
     * @param <T>      expected return type
     * @param jsonPath expression to evaluate
     * @param type     class to cast the result
     * @return extracted value
     */
    public <T> T extractJsonPath(String jsonPath, Class<T> type) {
        Response current = ensureResponse();
        return current.jsonPath().getObject(jsonPath, type);
    }

    /**
     * Obtains a header from the stored response.
     *
     * @param name header name
     * @return header value or null if not present
     */
    public String extractHeader(String name) {
        return ensureResponse().getHeader(name);
    }

    /**
     * Obtains a cookie from the stored response.
     *
     * @param name cookie name
     * @return cookie value or null if absent
     */
    public String extractCookie(String name) {
        return ensureResponse().getCookie(name);
    }

    /**
     * Executes a SELECT statement using the configured {@link DBHelper}.
     *
     * @param sql    query to run
     * @param params optional parameters
     * @return list of result rows
     */
    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        ensureDbHelper();
        return dbHelper.query(sql, params);
    }

    /**
     * Executes an INSERT/UPDATE/DELETE using the configured {@link DBHelper}.
     *
     * @param sql    statement to run
     * @param params optional parameters
     * @return number of affected rows
     */
    public int executeUpdate(String sql, Object... params) {
        ensureDbHelper();
        return dbHelper.execute(sql, params);
    }

    /**
     * Validates that the result of the provided query is empty.
     *
     * @param sql    query to validate
     * @param params optional parameters
     * @return client instance for chaining
     */
    public RestServiceClient validateQueryEmpty(String sql, Object... params) {
        List<Map<String, Object>> results = executeQuery(sql, params);
        if (!results.isEmpty()) {
            throw new AssertionError("La consulta retornó resultados cuando se esperaba vacío");
        }
        LOGGER.info("Validación exitosa: la consulta no retornó registros");
        return this;
    }

    /**
     * Extracts a value from a query result.
     *
     * @param sql        query to run
     * @param columnName column to extract from the first row
     * @param params     optional parameters
     * @return extracted value or {@code null} if no rows
     */
    public Object extractValueFromQuery(String sql, String columnName, Object... params) {
        List<Map<String, Object>> results = executeQuery(sql, params);
        if (results.isEmpty()) {
            return null;
        }
        Map<String, Object> firstRow = results.get(0);
        return firstRow.get(columnName);
    }

    /**
     * Provides access to the raw response for advanced assertions.
     *
     * @return last response
     */
    public Response getResponse() {
        return ensureResponse();
    }

    private void ensureDbHelper() {
        if (dbHelper == null) {
            throw new IllegalStateException("Debe configurar un DBHelper para ejecutar consultas");
        }
    }

    /**
     * Comparison operators for response time validations.
     */
    public enum TimeComparison {
        LESS_THAN,
        GREATER_THAN,
        EQUAL_TO
    }
}

