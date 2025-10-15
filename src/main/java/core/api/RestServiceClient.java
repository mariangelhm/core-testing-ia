package core.api;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private static final String BOX_TOP =
            "+========================================================================================+";
    private static final String BOX_DIVIDER =
            "+----------------------------------------------------------------------------------------+";
    private static final String BOX_SECTION =
            "|----------------------------------------------------------------------------------------|";
    private static final String BOX_BOTTOM =
            "+========================================================================================+";
    private static final String ALERT_TOP =
            "+========================================================================================+";
    private static final String ALERT_HEADER =
            "|>>>>>>>>>>>>>>>>>>>>>>>>>>>>  VALIDACIÓN FALLIDA DETECTADA  <<<<<<<<<<<<<<<<<<<<<<<<<<<<|";
    private static final String ALERT_DIVIDER =
            "+----------------------------------------------------------------------------------------+";
    private static final String ALERT_BOTTOM =
            "+========================================================================================+";
    private static final int PREVIEW_LIMIT = 180;

    private final RequestSpecBuilder specBuilder = new RequestSpecBuilder();
    private final Map<String, String> requestHeaders = new LinkedHashMap<>();
    private final Map<String, Object> requestQueryParams = new LinkedHashMap<>();
    private final Map<String, Object> requestPathParams = new LinkedHashMap<>();
    private final Map<String, Object> requestFormParams = new LinkedHashMap<>();
    private final Map<String, String> requestCookies = new LinkedHashMap<>();
    private final List<String> requestMultiparts = new ArrayList<>();
    private Method method = Method.GET;
    private String url;
    private boolean followRedirects = true;
    private Response response;
    private DBHelper dbHelper;
    private Object requestBody;
    private String requestBodyDescription;

    /**
     * Creates a new REST client with default request specification builders.
     */
    public RestServiceClient() {
        LOGGER.debug("RestServiceClient inicializado");
    }

    /**
     * Assigns the base URL for the request.
     *
     * @param url target endpoint
     * @return client instance for chaining
     */
    public RestServiceClient url(String url) {
        this.url = Objects.requireNonNull(url, "url must not be null");
        LOGGER.debug("URL configurada: {}", url);
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
        LOGGER.debug("Método HTTP configurado mediante enum: {}", method);
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
        LOGGER.debug("Método HTTP configurado mediante texto: {}", this.method);
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
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, "value must not be null");
        String sanitizedValue = value.toString();
        specBuilder.addHeader(name, sanitizedValue);
        requestHeaders.put(name, sanitizedValue);
        LOGGER.debug("Header agregado: {}={}", name, sanitizedValue);
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
        requestQueryParams.put(name, value);
        LOGGER.debug("Query param agregado: {}={}", name, value);
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
        requestPathParams.put(name, value);
        LOGGER.debug("Path param agregado: {}={}", name, value);
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
        requestBodyDescription = "Contenido con Content-Type " + contentType;
        LOGGER.debug("Content-Type configurado: {}", contentType);
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
        this.requestBody = body;
        this.requestBodyDescription = body == null ? "JSON vacío" : "JSON -> " + body;
        LOGGER.debug("Body JSON configurado: {}", requestBodyDescription);
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
        requestFormParams.putAll(formParams);
        requestBodyDescription = "Form-UrlEncoded -> " + formParams;
        LOGGER.debug("Form params configurados: {}", formParams);
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
        requestMultiparts.add(String.format("%s -> file[%s]", controlName, file != null ? file.getName() : "null"));
        requestBodyDescription = "Multipart -> " + requestMultiparts;
        LOGGER.debug("Multipart archivo agregado: {} -> {}", controlName, file);
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
        requestMultiparts.add(String.format("%s -> stream[%s, %s]", controlName, fileName, mimeType));
        requestBodyDescription = "Multipart -> " + requestMultiparts;
        LOGGER.debug("Multipart stream agregado: {} -> nombre={}, mimeType={}", controlName, fileName, mimeType);
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
        this.requestBody = bytes;
        this.requestBodyDescription = bytes == null ? "Binary vacío" : "Binary -> tamaño=" + bytes.length;
        LOGGER.debug("Body binario configurado ({} bytes, contentType={})", bytes != null ? bytes.length : 0, contentType);
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
        this.requestBody = body;
        this.requestBodyDescription = body == null ? "Body vacío" : "Body -> " + body;
        LOGGER.debug("Body configurado: {}", requestBodyDescription);
        return this;
    }

    /**
     * Adds a cookie to the request specification.
     *
     * @param name  cookie name
     * @param value cookie value
     * @return client instance for chaining
     */
    public RestServiceClient addCookie(String name, Object value) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, "value must not be null");
        String sanitizedValue = value.toString();
        specBuilder.addCookie(name, sanitizedValue);
        requestCookies.put(name, sanitizedValue);
        LOGGER.debug("Cookie agregada: {}={}", name, sanitizedValue);
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
        LOGGER.debug("Seguimiento de redirecciones configurado: {}", follow);
        return this;
    }

    /**
     * Associates a {@link DBHelper} instance to reuse database utilities from the same client.
     *
     * @param helper configured helper
     * @return client instance for chaining
     */
    public RestServiceClient withDBHelper(DBHelper helper) {
        this.dbHelper = Objects.requireNonNull(helper, "helper must not be null");
        LOGGER.debug("DBHelper asociado: {}", helper);
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
        RequestSpecification baseSpecification = specBuilder.build();
        RequestSpecification request = RestAssured.given(baseSpecification)
                .config(RestAssuredConfig.config()
                        .redirect(RedirectConfig.redirectConfig().followRedirects(followRedirects)));
        logRequest();
        long start = System.nanoTime();
        response = request.request(method, url);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        logResponse(elapsedMillis);
        return response;
    }

    private Response ensureResponse() {
        if (response == null) {
            throw new IllegalStateException("Debe ejecutar la solicitud antes de realizar validaciones");
        }
        LOGGER.debug("Respuesta disponible en memoria: status={}", response.getStatusCode());
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
            LinkedHashMap<String, Object> details = new LinkedHashMap<>();
            details.put("Petición", requestSummary());
            details.put("Status esperado", expectedStatus);
            details.put("Status recibido", actual);
            logAssertionFailure("Código de respuesta inesperado", details);
            throw new AssertionError(String.format("Código de respuesta esperado %d pero fue %d", expectedStatus, actual));
        }
        LOGGER.info("Validación exitosa de código de respuesta esperado={} actual={}", expectedStatus, actual);
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
            LinkedHashMap<String, Object> details = new LinkedHashMap<>();
            details.put("Petición", requestSummary());
            details.put("Texto esperado", expectedText);
            details.put("Body recibido", abbreviate(body));
            logAssertionFailure("Body sin texto esperado", details);
            throw new AssertionError("El cuerpo de la respuesta no contiene el texto esperado: " + expectedText);
        }
        LOGGER.info("Validación exitosa del contenido esperado en la respuesta (texto buscado='{}')", expectedText);
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
            LinkedHashMap<String, Object> details = new LinkedHashMap<>();
            details.put("Petición", requestSummary());
            details.put("jsonPath", jsonPath);
            details.put("Valor esperado", expected);
            details.put("Valor recibido", actual);
            logAssertionFailure("Valor JSON inesperado", details);
            throw new AssertionError(String.format("Valor esperado en '%s' era '%s' pero fue '%s'", jsonPath, expected, actual));
        }
        LOGGER.info("Validación exitosa para jsonPath '{}' con valor '{}'", jsonPath, actual);
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
            LinkedHashMap<String, Object> details = new LinkedHashMap<>();
            details.put("Petición", requestSummary());
            details.put("Comparación", comparator);
            details.put("Objetivo (ms)", target);
            details.put("Observado (ms)", elapsed);
            logAssertionFailure("Tiempo de respuesta fuera de rango", details);
            throw new AssertionError(String.format("Tiempo de respuesta %d ms no cumple con la condición %s %d ms", elapsed,
                    comparator, target));
        }
        LOGGER.info("Validación exitosa del tiempo de respuesta: observado={} ms, condición={} {} ms", elapsed, comparator,
                target);
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
        T value = current.jsonPath().getObject(jsonPath, type);
        LOGGER.debug("Valor extraído de jsonPath '{}': {}", jsonPath, value);
        return value;
    }

    /**
     * Obtains a header from the stored response.
     *
     * @param name header name
     * @return header value or null if not present
     */
    public String extractHeader(String name) {
        String value = ensureResponse().getHeader(name);
        LOGGER.debug("Header de respuesta extraído {}={}", name, value);
        return value;
    }

    /**
     * Obtains a cookie from the stored response.
     *
     * @param name cookie name
     * @return cookie value or null if absent
     */
    public String extractCookie(String name) {
        String value = ensureResponse().getCookie(name);
        LOGGER.debug("Cookie de respuesta extraída {}={}", name, value);
        return value;
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
        LOGGER.info("Ejecutando consulta desde RestServiceClient: {} con parámetros {}", sql, formatParams(params));
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
        LOGGER.info("Ejecutando actualización desde RestServiceClient: {} con parámetros {}", sql, formatParams(params));
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
            LinkedHashMap<String, Object> details = new LinkedHashMap<>();
            details.put("Consulta", abbreviate(sql));
            details.put("Parámetros", formatParams(params));
            details.put("Registros devueltos", results.size());
            logAssertionFailure("La consulta debía retornar vacío", details);
            throw new AssertionError("La consulta retornó resultados cuando se esperaba vacío");
        }
        LOGGER.info("Validación exitosa: la consulta '{}' no retornó registros", sql);
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
        Object value = firstRow.get(columnName);
        LOGGER.debug("Valor extraído de la consulta '{}' columna '{}': {}", sql, columnName, value);
        return value;
    }

    /**
     * Provides access to the raw response for advanced assertions.
     *
     * @return last response
     */
    public Response getResponse() {
        Response current = ensureResponse();
        LOGGER.debug("Respuesta recuperada para uso adicional: status={}", current.getStatusCode());
        return current;
    }

    private void ensureDbHelper() {
        if (dbHelper == null) {
            throw new IllegalStateException("Debe configurar un DBHelper para ejecutar consultas");
        }
        LOGGER.debug("DBHelper disponible para operaciones de base de datos");
    }

    private void logRequest() {
        LOGGER.info(BOX_TOP);
        logLine("INICIO SERVICIO", String.format("[%s] %s", method, url));
        LOGGER.info(BOX_DIVIDER);
        logLine("Headers", requestHeaders.isEmpty() ? "{}" : requestHeaders);
        logLine("Query params", requestQueryParams.isEmpty() ? "{}" : requestQueryParams);
        logLine("Path params", requestPathParams.isEmpty() ? "{}" : requestPathParams);
        logLine("Cookies", requestCookies.isEmpty() ? "{}" : requestCookies);
        Object bodyRepresentation = requestBodyDescription != null ? requestBodyDescription
                : (requestBody == null ? "<sin cuerpo>" : requestBody);
        logLine("Body", bodyRepresentation);
        if (!requestFormParams.isEmpty()) {
            logLine("Form params", requestFormParams);
        }
        if (!requestMultiparts.isEmpty()) {
            logLine("Multipart", requestMultiparts);
        }
        logLine("Redirecciones", followRedirects);
    }

    private void logResponse(long elapsedMillis) {
        LOGGER.info(BOX_SECTION);
        logLine("FIN SERVICIO", String.format("[%s] %s", method, url));
        LOGGER.info(BOX_DIVIDER);
        logLine("STATUS", String.format("%d | %d ms", response.getStatusCode(), elapsedMillis));
        logLine("Headers", response.getHeaders().asList());
        logLine("Cookies", response.getCookies());
        try {
            String body = response.getBody() != null ? response.getBody().asString() : "<sin cuerpo>";
            logLine("Body", toSingleLine(body));
        } catch (Exception ex) {
            LOGGER.warn("No fue posible formatear el cuerpo de la respuesta", ex);
        }
        LOGGER.info(BOX_BOTTOM);
    }

    private void logLine(String label, Object value) {
        String paddedLabel = String.format("%-15s", label);
        LOGGER.info("| {} : {}", paddedLabel, toSingleLine(value));
    }

    private void logAssertionFailure(String failureTitle, LinkedHashMap<String, Object> details) {
        LOGGER.error(ALERT_TOP);
        LOGGER.error(ALERT_HEADER);
        LOGGER.error(ALERT_DIVIDER);
        logFailureLine("Validación", failureTitle);
        if (details != null && !details.isEmpty()) {
            LOGGER.error(ALERT_DIVIDER);
            details.forEach(this::logFailureLine);
        }
        StackTraceElement origin = resolveFailureOrigin();
        if (origin != null) {
            LOGGER.error(ALERT_DIVIDER);
            logFailureLine("Ubicación", formatLocation(origin));
        }
        LOGGER.error(ALERT_BOTTOM);
    }

    private void logFailureLine(String label, Object value) {
        String paddedLabel = String.format("%-20s", label);
        LOGGER.error("| {} : {}", paddedLabel, toSingleLine(value));
    }

    private StackTraceElement resolveFailureOrigin() {
        String thisClass = RestServiceClient.class.getName();
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (className.equals(thisClass) || className.startsWith("java.lang") || className.startsWith("jdk.internal")) {
                continue;
            }
            return element;
        }
        return null;
    }

    private String formatLocation(StackTraceElement origin) {
        return origin.getClassName() + "#" + origin.getMethodName() + ":" + origin.getLineNumber();
    }

    private String requestSummary() {
        String httpMethod = method != null ? method.name() : "<sin método>";
        String endpoint = url != null ? url : "<sin url>";
        return httpMethod + " " + endpoint;
    }

    private String abbreviate(Object value) {
        String text = toSingleLine(value);
        if (text.length() <= PREVIEW_LIMIT) {
            return text;
        }
        return text.substring(0, PREVIEW_LIMIT - 3) + "...";
    }

    private String toSingleLine(Object value) {
        if (value == null) {
            return "<null>";
        }
        String text = String.valueOf(value);
        String collapsed = text.replaceAll("\\s*\r?\n\\s*", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        return collapsed.isEmpty() ? "<vacío>" : collapsed;
    }

    private String formatParams(Object... params) {
        if (params == null || params.length == 0) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            builder.append(param);
            if (i < params.length - 1) {
                builder.append(", ");
            }
        }
        return builder.append(']').toString();
    }

    /**
     * Comparison operators for response time validations.
     */
    public enum TimeComparison {
        /** Validates the response time is strictly lower than the expected value. */
        LESS_THAN,
        /** Validates the response time is strictly greater than the expected value. */
        GREATER_THAN,
        /** Validates the response time matches the expected value. */
        EQUAL_TO
    }
}

