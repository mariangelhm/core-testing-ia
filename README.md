# web-testing-ia

Backend base para gestionar proyectos de automatización web sin base de datos. Detecta proyectos por carpeta, administra casos Gherkin, locators en YAML, aplica reglas de calidad, expone APIs REST y ofrece una grabadora multiusuario de flujos web.

## Arquitectura
- **Spring Boot 3 / Java 17** con controladores REST en `com.example.webtestingia.controller`.
- **Servicios de dominio** en `com.example.webtestingia.service` para proyectos, casos, locators, calidad y grabadora.
- **Drivers Selenium** gestionados por `drivers/WebDriverFactory` leyendo `config/navegador.yml`.
- **Quality analyzer** en `quality/QualityAnalyzer` cargando reglas activas desde `config/quality-rules.yml`.
- **Grabadora** en `recorder/` con `RecorderSessionManager`, `RecorderService` y `StepMapper` para traducir eventos a pasos Gherkin.
- **Steps genéricos y precondiciones** en `steps/`, reutilizables desde Cucumber.

## Detección de proyectos
- Cada carpeta dentro de `src/test/resources/features/` es un proyecto.
- Debe contener `project.json` con la forma:
```json
{
  "id": "uuid",
  "nombre": "Portal clientes",
  "codigoJira": "PORTAL",
  "tipo": "WEB",
  "autor": "Nombre",
  "editor": "Nombre",
  "casos": []
}
```
- Si no existe, `ProjectDiscoveryService` lo crea con valores por defecto.

## Casos Gherkin
- Los casos viven en subcarpetas por funcionalidad: `src/test/resources/features/<proyecto>/<funcionalidad>/*.feature`.
- `CaseFileService` permite listar, leer, crear, actualizar y eliminar archivos `.feature`, además de calcular la calidad con `QualityAnalyzer`.
- `CasoWebController` expone APIs REST para gestionar los casos y entregar calidad detallada.

## Locators YAML
- Ubicados en `src/main/resources/locators/<proyecto>/*.yml`.
- Ejemplo `locator.login.yml`:
```yaml
login:
  input_user: "css=input#user"
  btn_ingresar: "css=#login"
registro:
  input_cedula: "xpath=//input[@id='cedula']"
```
- `LocatorService` cachea el contenido y resuelve selectors por grupo y nombre.

## Steps genéricos
- `WebGenericSteps` permite usar **locators por nombre** (apoyados en YAML) o **selectores directos** (`//xpath`, `css=...`).
- Pasos clave:
  - `Given establezco el grupo "<grupo>"`
  - `When hago clic en "<objetivo>"`
  - `When escribo "<texto>" en "<objetivo>"`
  - `Then debería ver el texto "<texto>"`
- `PreconditionSteps` usa `qa-core` para queries/servicios previos (`DBHelper`, `RestServiceClient`).
- Todas las acciones usan `WebDriverWait` para esperas automáticas.

## Reglas de calidad
- Configuradas en `src/main/resources/config/quality-rules.yml` con campos `id`, `nombre`, `descripcion`, `activo`, `peso`.
- Solo se ejecutan las reglas activas; el resultado incluye `puntaje`, `reglasCumplidas`, `reglasFalladas` y `sugerencias`.
- Se usa al listar casos, consultar un caso o calcular la calidad promedio de un proyecto.

## Grabador multiusuario
- Endpoints en `RecorderController`:
  - `POST /api/recorder/start` crea `sessionId` y abre un navegador dedicado.
  - `POST /api/recorder/event` recibe eventos (`click`, `input`, `change`, `navigate`, etc.) y los convierte con `StepMapper`.
  - `GET /api/recorder/steps` devuelve pasos acumulados.
  - `POST /api/recorder/stop` cierra la sesión, devuelve los pasos y el resumen de calidad.
- `RecorderSessionManager` mantiene sesiones aisladas en memoria.
- `StepMapper` detecta si el objetivo es locator por nombre o selector directo y genera líneas Gherkin compatibles.

## Configuración de navegador
- `config/navegador.yml` define `tipo` (chrome/firefox/edge), `modo` (local|grid), `auto_download_drivers` y `grid_url`.
- `WebDriverFactory` prepara drivers locales o remotos usando WebDriverManager cuando aplica.

## Ejecución con Cucumber
- Tarea Gradle `runCucumber`:
```bash
./gradlew runCucumber -Pcucumber.tags="@smoke"
```
- Usa glue `com.example.webtestingia.steps` y `com.example.webtestingia.hooks` sobre `src/test/resources/features`.

## Logging y manejo de errores
- Todas las clases incluyen comentarios en español y logs informativos.
- `GlobalExceptionHandler` diferencia errores de proyecto, archivo, configuración, locators, Gherkin y grabadora con códigos HTTP apropiados.
