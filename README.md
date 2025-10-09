# qa-core

Biblioteca centralizada de utilidades para los microservicios de pruebas de QA. Provee configuración compartida,
logs, integración con Jira/Xray, Jenkins y acceso a base de datos.

> **Requisito**: compilar y ejecutar con Java 17.

## Módulos principales

- **ConfigManager**: carga propiedades desde archivos `application.properties`, variables de entorno y parámetros `-D`.
- **LoggerUtil**: expone métodos comunes para SLF4J/Logback y permite ajustar el nivel de logs.
- **Errores**: excepciones personalizadas `FrameworkException`, `JiraException`, `JenkinsException`, `DBException`.
- **JiraClient**: operaciones REST para crear tests, agregar a ejecuciones y reportar resultados.
- **JenkinsClient**: helpers para disparar jobs, consultar estado y obtener logs.
- **DBHelper**: pool de conexiones vía HikariCP y métodos `query`/`execute`.
- **Utils**: generador de IDs, utilidades de fechas, archivos y datos aleatorios.

## Uso

Agregar la dependencia en otros proyectos internos tras publicar el artefacto.

```groovy
implementation 'com.company.qa:qa-core:1.0.0'
```

### Publicar el artefacto de forma local

Si querés consumir `qa-core` desde otro servicio sin subirlo a un repositorio corporativo,
podés publicarlo en tu `~/.m2` local con Gradle y luego resolverlo desde ahí.

1. Asegurate de tener instalado **JDK 17** y Gradle 8+ (o usar el *wrapper* si está disponible).
2. Desde la raíz del proyecto ejecutá:

   ```bash
   gradle clean publishToMavenLocal
   # o ./gradlew clean publishToMavenLocal
   ```

   Esto compila el proyecto, genera el JAR en `build/libs/` y lo instala en
   `~/.m2/repository/com/company/qa/qa-core/1.0.0`.
3. En el microservicio consumidor agregá `mavenLocal()` antes de `mavenCentral()` para
   que tome el artefacto local:

   ```groovy
   repositories {
       mavenLocal()
       mavenCentral()
   }

   dependencies {
       implementation 'com.company.qa:qa-core:1.0.0'
   }
   ```

Cuando hagas cambios en `qa-core`, incrementá la versión en `build.gradle` y volvé a ejecutar
`publishToMavenLocal` para que el servicio consumidor obtenga la nueva build.

### Publicar en GitHub Packages

Si querés tener el artefacto disponible en línea, podés subirlo a GitHub Packages.

1. Creá un token personal con permisos `write:packages`, `read:packages` y `repo`.
2. Configurá tus credenciales en `~/.gradle/gradle.properties` o como variables de entorno:

   ```properties
   gpr.user=TU_USUARIO
   gpr.key=TOKEN_GENERADO
   ```

   o bien exportá `GITHUB_USERNAME`/`GITHUB_TOKEN` antes de publicar.
3. Editá `build.gradle` reemplazando `OWNER/REPO` por la organización y repositorio que alojarán el paquete.
4. Ejecutá la publicación:

   ```bash
   gradle clean publish
   # o ./gradlew clean publish
   ```

   Gradle subirá el JAR a `https://maven.pkg.github.com/OWNER/REPO`.
5. En los proyectos consumidores agregá el repositorio de GitHub con credenciales:

   ```groovy
   repositories {
       maven {
           url = uri("https://maven.pkg.github.com/OWNER/REPO")
           credentials {
               username = findProperty("gpr.user") ?: System.getenv("GITHUB_USERNAME")
               password = findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
           }
       }
       mavenCentral()
   }
   ```

   Luego declarás la dependencia normalmente con `implementation 'com.company.qa:qa-core:1.0.0'`.

### Configuración

Definir el entorno con `-Dqa.env=qa` o variable `ENV`. Los archivos `application-<env>.properties`
se pueden ubicar en el classpath o directorio de trabajo.

### Logging

El nivel de logs se puede modificar dinámicamente:

```java
LoggerUtil.setRootLevel("DEBUG");
```

Los logs se escriben en consola y en `logs/qa-core.log`.
