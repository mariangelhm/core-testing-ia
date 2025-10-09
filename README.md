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

### Configuración

Definir el entorno con `-Dqa.env=qa` o variable `ENV`. Los archivos `application-<env>.properties`
se pueden ubicar en el classpath o directorio de trabajo.

### Logging

El nivel de logs se puede modificar dinámicamente:

```java
LoggerUtil.setRootLevel("DEBUG");
```

Los logs se escriben en consola y en `logs/qa-core.log`.
