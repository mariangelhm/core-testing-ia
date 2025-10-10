# qa-core

Biblioteca centralizada de utilidades para los microservicios de QA automatizada. El objetivo del módulo es
unificar la configuración de entornos, el logging, la conexión con herramientas externas (Jira/Xray y
Jenkins) y el acceso a bases de datos para evitar duplicación de código en los diferentes proyectos de
pruebas.

> **Requisito**: compilar y ejecutar con **Java 17**.

## Contenido de la librería

| Módulo | Paquete | Qué resuelve |
| --- | --- | --- |
| **Gestión de configuración** | `core.config.ConfigManager` | Carga propiedades desde archivos `application.properties` y variantes por entorno (`application-<env>.properties`), variables de entorno y parámetros `-D`. Permite priorizar overrides en tiempo de ejecución. |
| **Logging centralizado** | `core.log.LoggerUtil` | Expone un `Logger` compartido con SLF4J/Logback, manejo de niveles dinámico y configuración de appenders para consola y archivo (`logs/qa-core.log`). |
| **Manejo de errores** | `core.errors` | Excepciones personalizadas (`FrameworkException`, `JiraException`, `JenkinsException`, `DBException`) para encapsular fallos y enriquecer mensajes/logs. |
| **Cliente Jira/Xray** | `core.jira.JiraClient` | Operaciones REST para crear casos de prueba, asociarlos a ejecuciones y reportar resultados o consultar estados. Usa Jackson para serialización y OkHttp como cliente HTTP. |
| **Cliente Jenkins** | `core.jenkins.JenkinsClient` | Métodos para disparar jobs parametrizados, consultar builds y recuperar logs de ejecución vía API REST. |
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

## Acceso a base de datos

```java
try (DBHelper db = new DBHelper()) {
    List<Map<String, Object>> rows = db.query("SELECT * FROM users WHERE status = ?", "ACTIVE");
    int updated = db.execute("UPDATE users SET last_login = NOW() WHERE id = ?", 1001);
}
```

La conexión usa HikariCP. Las claves esperadas son `db.url`, `db.user`, `db.password`, `db.pool.maxSize`,
entre otras. `DBHelper` cierra automáticamente los recursos mediante `AutoCloseable`.

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
