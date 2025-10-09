# qa-core

Biblioteca centralizada de utilidades para los microservicios de pruebas de QA. Provee configuración compartida,
logs, integración con Jira/Xray, Jenkins y acceso a base de datos.

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

### Configuración

Definir el entorno con `-Dqa.env=qa` o variable `ENV`. Los archivos `application-<env>.properties`
se pueden ubicar en el classpath o directorio de trabajo.

### Logging

El nivel de logs se puede modificar dinámicamente:

```java
LoggerUtil.setRootLevel("DEBUG");
```

Los logs se escriben en consola y en `logs/qa-core.log`.
