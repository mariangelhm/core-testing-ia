package com.example.webtestingia.service;

import com.example.webtestingia.model.CaseDetail;
import com.example.webtestingia.model.CaseSummary;
import com.example.webtestingia.model.exception.FileProcessingException;
import com.example.webtestingia.model.exception.GherkinParseException;
import com.example.webtestingia.model.exception.ProjectNotFoundException;
import com.example.webtestingia.quality.QualityAnalyzer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Gestiona el ciclo de vida de los archivos .feature sin necesidad de BD.
 */
@Service
public class CaseFileService {

    private static final Logger log = LoggerFactory.getLogger(CaseFileService.class);
    private static final String FEATURES_PATH = "src/test/resources/features";

    private final QualityAnalyzer qualityAnalyzer;

    public CaseFileService(QualityAnalyzer qualityAnalyzer) {
        this.qualityAnalyzer = qualityAnalyzer;
    }

    /**
     * Lista todos los casos de un proyecto recorriendo sus subcarpetas.
     *
     * @param proyecto nombre de la carpeta de proyecto.
     * @return resumen de cada escenario encontrado.
     */
    public List<CaseSummary> listarCasos(String proyecto) {
        Path base = Path.of(FEATURES_PATH, proyecto);
        if (!Files.isDirectory(base)) {
            throw new ProjectNotFoundException("Proyecto no encontrado: " + proyecto);
        }
        try {
            List<Path> features = Files.walk(base)
                    .filter(p -> p.toString().endsWith(".feature"))
                    .collect(Collectors.toList());
            List<CaseSummary> casos = new ArrayList<>();
            for (Path feature : features) {
                casos.addAll(parseFeature(feature));
            }
            return casos;
        } catch (IOException e) {
            throw new FileProcessingException("No se pudieron listar los casos", e);
        }
    }

    /**
     * Lee un archivo .feature y devuelve su contenido con calidad.
     */
    public CaseDetail leerCaso(String proyecto, String rutaRelativa) {
        Path path = resolveFeaturePath(proyecto, rutaRelativa);
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            QualityResult calidad = qualityAnalyzer.analizarCaso(content);
            CaseDetail detail = new CaseDetail();
            detail.setContenido(content);
            detail.setCalidad(calidad);
            return detail;
        } catch (IOException e) {
            throw new FileProcessingException("Error leyendo el caso solicitado", e);
        }
    }

    /**
     * Crea un nuevo archivo .feature o agrega un escenario.
     */
    public void crearCaso(String proyecto, String rutaRelativa, String contenido) {
        Path path = resolveFeaturePath(proyecto, rutaRelativa);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, contenido, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileProcessingException("No se pudo crear el caso", e);
        }
    }

    /**
     * Actualiza el contenido de un archivo existente.
     */
    public void actualizarCaso(String proyecto, String rutaRelativa, String contenido) {
        Path path = resolveFeaturePath(proyecto, rutaRelativa);
        if (!Files.exists(path)) {
            throw new FileProcessingException("El archivo no existe: " + rutaRelativa, null);
        }
        try {
            Files.writeString(path, contenido, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileProcessingException("No se pudo actualizar el caso", e);
        }
    }

    /**
     * Elimina un archivo .feature completo.
     */
    public void eliminarCaso(String proyecto, String rutaRelativa) {
        Path path = resolveFeaturePath(proyecto, rutaRelativa);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new FileProcessingException("No se pudo eliminar el caso", e);
        }
    }

    /**
     * Compone la ruta absoluta del archivo .feature solicitado.
     *
     * @param proyecto nombre de proyecto.
     * @param rutaRelativa ruta relativa dentro del proyecto.
     * @return ruta absoluta del archivo.
     */
    private Path resolveFeaturePath(String proyecto, String rutaRelativa) {
        if (StringUtils.isBlank(rutaRelativa)) {
            throw new GherkinParseException("La ruta del caso es obligatoria");
        }
        return Path.of(FEATURES_PATH, proyecto, rutaRelativa);
    }

    /**
     * Descompone un archivo .feature en resúmenes por escenario.
     *
     * @param feature ruta del archivo.
     * @return lista de resúmenes con calidad.
     */
    private List<CaseSummary> parseFeature(Path feature) {
        try {
            String content = Files.readString(feature, StandardCharsets.UTF_8);
            List<String> tags = extractTags(content);
            List<String> escenarios = extractScenarios(content);
            List<CaseSummary> summaries = new ArrayList<>();
            for (String escenario : escenarios) {
                CaseSummary summary = new CaseSummary();
                summary.setRuta(Path.of(FEATURES_PATH).relativize(feature).toString());
                summary.setEscenario(escenario);
                summary.setTags(tags);
                summary.setCalidad(qualityAnalyzer.analizarCaso(content));
                summaries.add(summary);
            }
            return summaries;
        } catch (IOException e) {
            throw new FileProcessingException("No se pudo leer el archivo " + feature, e);
        }
    }

    /**
     * Extrae todos los tags definidos en el archivo Gherkin.
     */
    private List<String> extractTags(String content) {
        List<String> tags = new ArrayList<>();
        Pattern pattern = Pattern.compile("^(\\s*@.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            tags.add(matcher.group().trim());
        }
        return tags;
    }

    /**
     * Obtiene los nombres de escenario declarados en el archivo.
     */
    private List<String> extractScenarios(String content) {
        List<String> escenarios = new ArrayList<>();
        Pattern pattern = Pattern.compile("Scenario:(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            escenarios.add(matcher.group(1).trim());
        }
        if (escenarios.isEmpty()) {
            escenarios.add("Escenario sin título");
        }
        return escenarios;
    }
}
