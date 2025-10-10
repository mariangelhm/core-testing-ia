# qa-core

Biblioteca centralizada de utilidades para los microservicios de QA automatizada. El objetivo del módulo es
unificar la configuración de entornos, el logging, la conexión con herramientas externas (Jira/Xray y
Jenkins) y el acceso a bases de datos para evitar duplicación de código en los diferentes proyectos de
pruebas.

> **Requisito**: compilar y ejecutar con **Java 17**.
>
> El build de Gradle ya está configurado con un *toolchain* que descarga un JDK 17 si no está disponible en
> tu máquina. Al importar el proyecto como **Gradle Project** en IntelliJ IDEA/Eclipse o al ejecutar
> `gradle build` se resuelven automáticamente las clases estándar (`java.time`, `java.nio`, etc.). Si el IDE
> marca imports como `Instant` u `IOException` como "Cannot resolve symbol", asegurate de que el proyecto se
> abra con Gradle y que el IDE apunte al SDK 17 provisto por el toolchain (pestaña *Project Structure* → SDK).

## Contenido de la librería

| Módulo | Paquete | Qué resuelve |
| --- | --- | --- |
| **Gestión de configuración** | `core.config.ConfigManager` | Carga propiedades desde archivos `application.properties` y variantes por entorno (`application-<env>.properties`), variables de entorno y parámetros `-D`. Permite priorizar overrides en tiempo de ejecución. |
| **Logging centralizado** | `core.log.LoggerUtil` | Expone un `Logger` compartido con SLF4J/Logback, manejo de niveles dinámico y configuración de appenders para consola y archivo (`logs/qa-core.log`). |
| **Manejo de errores** | `core.errors` | Excepciones personalizadas (`FrameworkException`, `JiraException`, `JenkinsException`, `DBException`) para encapsular fallos y enriquecer mensajes/logs. |
| **Cliente Jira/Xray** | `core.jira.JiraClient` | Operaciones REST para crear casos de prueba, asociarlos a ejecuciones y reportar resultados o consultar estados. Usa Jackson para serialización y OkHttp como cliente HTTP. |
| **Cliente Jenkins** | `core.jenkins.JenkinsClient` | Métodos para disparar jobs parametrizados, consultar builds y recuperar logs de ejecución vía API REST. |
| **Cliente REST funcional** | `core.api.RestServiceClient` | Construcción fluida de requests con Rest Assured, validaciones de respuesta, extracción de datos, manejo de redirecciones y reutilización de consultas SQL con `DBHelper`. |
| **Acceso a base de datos** | `core.db.DBHelper` | Pool de conexiones con HikariCP, helpers `query`/`execute`, conversión de resultados a listas/mapas y manejo de credenciales por entorno. |
| **Utilidades** | `core.utils` | Funciones reutilizables: generación de IDs (`UniqueIdGenerator`), fechas (`DateUtil`), archivos (`FileUtil`) y datos aleatorios (`RandomDataUtil`). |

## Cómo consumir la librería

El artefacto se publica en GitHub Packages con el identificador `com.company.qa:qa-core:1.0.0`.

1. Agregá el repositorio de GitHub Packages en tu `build.gradle` (o `settings.gradle` para Gradle 8+):

   ```groovy
   repositories {
       maven {
           url = uri("https://maven.pkg.github.com/mariangelhm/qa-core")
           credentials {
               username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_USERNAME")
               password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
           }
       }
       mavenCentral()
   }
   ```

2. Declarar la dependencia en el módulo que vaya a reutilizar los servicios compartidos:

   ```groovy
   dependencies {
       implementation 'com.company.qa:qa-core:1.0.0'
   }
   ```

3. Proveé las credenciales como propiedades de Gradle (`~/.gradle/gradle.properties`) o variables de entorno
   (`GITHUB_USERNAME`, `GITHUB_TOKEN`). El token debe tener permisos `read:packages` (y `write:packages` si
   vas a publicar versiones nuevas).

## Configuración de entornos

`ConfigManager` resuelve los valores en el siguiente orden (de menor a mayor prioridad):

1. Archivo base `application.properties` (incluido en `src/main/resources`).
2. Archivo específico por entorno `application-<env>.properties` según `qa.env` o `ENV`.
3. Variables de entorno del sistema.
4. Propiedades pasadas por JVM (`-Dclave=valor`).

Ejemplo de uso:

```java
ConfigManager config = ConfigManager.getInstance();
String jiraUrl = config.get("jira.url");
String dbUser = config.get("db.user", "qa_user");
```

## Logging unificado

`LoggerUtil` expone utilidades para crear loggers consistentes:

```java
import core.log.LoggerUtil;
import org.slf4j.Logger;

Logger log = LoggerUtil.getLogger(MyClass.class);

log.info("Iniciando prueba");
LoggerUtil.setRootLevel("DEBUG");
```

Los logs se envían a la consola y al archivo `logs/qa-core.log`. El patrón y niveles se definen en
`src/main/resources/logback.xml`.

## Integraciones con Jira/Xray

```java
JiraClient jiraClient = new JiraClient();
String testKey = jiraClient.createTest("QA", "Feature: Demo\n  Scenario: Valid login");
jiraClient.addTestToExecution("EXEC-123", testKey);
jiraClient.reportResult("EXEC-123", testKey, "PASS");
```

Las credenciales y URL se leen de `ConfigManager` (`jira.url`, `jira.user`, `jira.token`).

## Integraciones con Jenkins

```java
JenkinsClient jenkinsClient = new JenkinsClient();
jenkinsClient.triggerJob("qa-pipeline", Map.of("ENV", "qa"));
String status = jenkinsClient.getJobStatus("qa-pipeline", 42);
String logs = jenkinsClient.getJobLogs("qa-pipeline", 42);
```

Configurable vía `jenkins.url`, `jenkins.user`, `jenkins.token`.

## Cliente REST para servicios externos

`RestServiceClient` encapsula las operaciones más comunes al consumir APIs REST desde pruebas automatizadas. El cliente soporta la definición de URL, método, encabezados, parámetros, payloads en distintos formatos y validaciones posteriores sobre la respuesta o la base de datos.

```java
import core.api.RestServiceClient;
import core.api.RestServiceClient.TimeComparison;
import core.db.DBHelper;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

DBHelper dbHelper = new DBHelper("jdbc:postgresql://host:5432/db", "user", "pass");

RestServiceClient client = new RestServiceClient()
        .url("https://api.qa.company.com/v1/users")
        .method("POST")
        .addHeader("Authorization", "Bearer " + token)
        .addQueryParam("notify", true)
        .jsonBody(Map.of("name", "QA Bot", "email", "bot@qa.com"))
        .withDBHelper(dbHelper)
        .followRedirects(false);

client.execute()
      .then()
      .log().all(); // acceso opcional al Response original

client.validateStatusCode(201)
      .validateBodyContains("QA Bot")
      .validateJsonPathEquals("data.properties[0].id", 12345)
      .validateResponseTime(Duration.ofSeconds(2), TimeComparison.LESS_THAN)
      .validateJsonSchema(Path.of("schemas/user-created.json"));

String userId = client.extractJsonPath("data.properties[0].id", String.class);
String requestId = client.extractHeader("X-Request-Id");

client.executeQuery("SELECT * FROM users WHERE id = ?", userId);
client.validateQueryEmpty("SELECT * FROM audit WHERE status = ?", "ERROR");
Object sessionId = client.extractValueFromQuery("SELECT session_id FROM sessions WHERE user_id = ?", "session_id", userId);
```

### Métodos disponibles

- **Declarar URL y método**: `url(String)` y `method(String|Method)`.
- **Headers y parámetros**: `addHeader`, `addQueryParam`, `addPathParam`.
- **Cuerpos de la solicitud**:
  - JSON con `jsonBody(Object)`.
  - Formularios `x-www-form-urlencoded` vía `formBody(Map)`.
  - Multiparte con `multiPart(...)` (acepta `File` o `InputStream`).
  - Binario arbitrario con `binaryBody(byte[], String)` o `body(Object)` para casos personalizados.
- **Control de redirecciones**: `followRedirects(boolean)` permite forzar o evitar redirecciones 302.
- **Validaciones**:
  - `validateStatusCode(int)` para códigos HTTP.
  - `validateBodyContains(String)` para textos.
  - `validateJsonPathEquals(String, Object)` para estructuras específicas (`data.properties[0].id`).
  - `validateResponseTime(Duration, TimeComparison)` para tiempos menor/mayor/igual a un umbral.
  - `validateJsonSchema(Path)` contra esquemas JSON.
- **Extracciones**:
  - `extractJsonPath`, `extractHeader`, `extractCookie` para datos de la respuesta.
  - `getResponse()` devuelve el `Response` completo de Rest Assured para validaciones avanzadas.
- **Integración con base de datos** (requiere `withDBHelper(DBHelper)`):
  - `executeQuery`/`executeUpdate` para consultas `SELECT` o `INSERT/UPDATE/DELETE`.
  - `extractValueFromQuery` para recuperar una columna específica.
  - `validateQueryEmpty` para confirmar que una consulta no devuelve registros.

## Acceso a base de datos

`core.db.DBHelper` encapsula el pool de conexiones (HikariCP) y expone helpers atómicos para lectura,
escritura y validaciones. Se puede construir con un `DataSource` externo o pasando las credenciales
directamente:

```java
DBHelper db = new DBHelper("jdbc:postgresql://host:5432/db", "user", "pass");
```

### Métodos disponibles

| Método | Descripción | Ejemplo |
| --- | --- | --- |
| `select(sql, params...)` | Ejecuta un `SELECT` y retorna una lista de filas (`List<Map<String,Object>>`). | `List<Map<String, Object>> activos = db.select("SELECT id, email FROM users WHERE status = ?", "ACTIVE");` |
| `selectFirst(sql, params...)` | Entrega la primera fila o `null` si no hay resultados. | `Map<String, Object> usuario = db.selectFirst("SELECT * FROM users WHERE id = ?", 1001);` |
| `selectValue(sql, params...)` | Devuelve el primer valor de la primera fila, ideal para agregaciones. | `Integer total = db.selectValue("SELECT COUNT(1) FROM users WHERE status = ?", "ACTIVE");` |
| `extractValue(rows, rowIndex, column)` | Permite tomar un valor específico de un resultado preexistente. | `String email = (String) db.extractValue(activos, 0, "email");` |
| `update(sql, params...)` / `execute` | Ejecuta `INSERT/UPDATE/DELETE` retornando filas afectadas. | `int updated = db.update("UPDATE users SET last_login = NOW() WHERE id = ?", 1001);` |
| `validateValueEquals(sql, expected, params...)` | Compara el valor retornado por la consulta contra un esperado y devuelve `true/false`. | `boolean ok = db.validateValueEquals("SELECT status FROM users WHERE id = ?", "ACTIVE", 1001);` |
| `assertValueEquals(sql, expected, params...)` | Igual que la anterior pero lanza `DBException` si no coincide. | `db.assertValueEquals("SELECT status FROM users WHERE id = ?", "ACTIVE", 1001);` |
| `isEmpty(sql, params...)` | Comprueba que una consulta no retorne filas. | `boolean vacio = db.isEmpty("SELECT 1 FROM users WHERE status = ?", "DELETED");` |
| `assertEmpty(sql, params...)` | Lanza excepción si la consulta arroja filas. | `db.assertEmpty("SELECT 1 FROM users WHERE email = ?", "duplicated@company.com");` |

### Buenas prácticas

- Encerrá el helper en un bloque `try-with-resources` si lo creás directamente para liberar el pool:
  ```java
  try (DBHelper db = new DBHelper(url, user, pass)) {
      // uso
  }
  ```
- Cuando se usa con `RestServiceClient`, podés inyectarlo vía `withDBHelper(DBHelper)` para reusar
  conexiones.
- Las propiedades soportadas por defecto son `db.url`, `db.user`, `db.password` y `db.pool.maxSize`.

## Publicar artefactos

### Publicación local (para pruebas rápidas)

```bash
gradle clean publishToMavenLocal
# o ./gradlew clean publishToMavenLocal
```

Esto instala el JAR en `~/.m2/repository/com/company/qa/qa-core/1.0.0` para ser consumido con `mavenLocal()`.

### Publicación en GitHub Packages

El `build.gradle` ya incluye el bloque `publishing` apuntando a GitHub Packages. Asegurate de actualizar el
`group`, `version` y la URL si cambiás de repositorio.

```bash
gradle clean publish
```

Gradle subirá el artefacto firmado con tus credenciales. Luego, los proyectos consumidores sólo deben
agregar el repositorio y la dependencia como se mostró arriba.

## Versionado y buenas prácticas

- Incrementá la versión en `build.gradle` cada vez que cambies la API pública.
- Mantené documentadas las variables de configuración esperadas en los servicios consumidores.
- Ejecutá las pruebas (`gradle test`) antes de publicar para validar regresiones.

## Estructura del repositorio

```
qa-core/
├── src/main/java/core/
│   ├── api/
│   ├── config/
│   ├── db/
│   ├── errors/
│   ├── jenkins/
│   ├── jira/
│   ├── log/
│   └── utils/
├── src/main/resources/
│   ├── application.properties
│   └── logback.xml
├── build.gradle
└── settings.gradle
```

Con esta guía podés integrar `qa-core` en tus microservicios de QA para reutilizar toda la infraestructura
común de configuración, logging, integraciones externas y utilidades.
