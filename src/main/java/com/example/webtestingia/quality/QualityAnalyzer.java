package com.example.webtestingia.quality;

import com.example.webtestingia.model.QualityResult;
import com.example.webtestingia.model.exception.QualityConfigException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Ejecuta las reglas activas de quality-rules.yml sobre un contenido Gherkin.
 */
@Component
public class QualityAnalyzer {

    private final List<QualityRule> reglasActivas;

    public QualityAnalyzer() {
        this.reglasActivas = cargarReglas();
    }

    /**
     * Analiza un texto Gherkin y devuelve el resultado ponderado.
     * @param featureContent contenido del caso.
     * @return resultado con puntaje y sugerencias.
     */
    public QualityResult analizarCaso(String featureContent) {
        QualityResult result = new QualityResult();
        double puntajeAcumulado = 0;
        double pesoTotal = 0;
        for (QualityRule regla : reglasActivas) {
            pesoTotal += regla.getPeso();
            boolean cumple = evaluarRegla(regla, featureContent);
            if (cumple) {
                puntajeAcumulado += regla.getPeso();
                result.getReglasCumplidas().add(regla.getId());
            } else {
                result.getReglasFalladas().add(regla.getId());
                result.getSugerencias().add(regla.getDescripcion());
            }
        }
        double puntaje = pesoTotal == 0 ? 0 : puntajeAcumulado / pesoTotal;
        result.setPuntaje(Math.round(puntaje * 100.0) / 100.0);
        return result;
    }

    /**
     * Evalúa una regla individual sobre el contenido Gherkin.
     *
     * @param regla     regla activa.
     * @param contenido texto del escenario.
     * @return true si la regla se cumple.
     */
    private boolean evaluarRegla(QualityRule regla, String contenido) {
        String lower = contenido.toLowerCase();
        switch (regla.getId()) {
            case "R1":
            case "R4":
                return lower.contains("then ") || lower.contains("then\n") || lower.contains("\nthen");
            case "R2":
                return Pattern.compile("Scenario:(.+)").matcher(contenido).find();
            case "R3":
                long pasos = Pattern.compile("^(Given|When|Then|And)", Pattern.MULTILINE).matcher(contenido).results().count();
                return pasos <= 20;
            case "R5":
                return lower.contains("given") && lower.contains("when") && lower.contains("then");
            default:
                return true;
        }
    }

    /**
     * Carga las reglas desde el YAML respetando el indicador de actividad.
     *
     * @return lista de reglas activas.
     */
    private List<QualityRule> cargarReglas() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config/quality-rules.yml")) {
            if (is == null) {
                throw new QualityConfigException("No se encontró config/quality-rules.yml");
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            java.util.Map<String, QualityRulesConfig> root = mapper.readValue(is,
                    mapper.getTypeFactory().constructMapType(java.util.Map.class, String.class, QualityRulesConfig.class));
            QualityRulesConfig config = root.get("quality");
            if (config == null) {
                throw new QualityConfigException("El nodo 'quality' no está presente en el YAML");
            }
            validarReglas(config.getReglas());
            List<QualityRule> activas = new ArrayList<>();
            for (QualityRule rule : config.getReglas()) {
                if (rule.isActivo()) {
                    activas.add(rule);
                }
            }
            return activas;
        } catch (IOException e) {
            throw new QualityConfigException("No se pudieron cargar las reglas de calidad", e);
        }
    }

    /**
     * Valida duplicados y pesos de las reglas cargadas.
     *
     * @param reglas conjunto completo proveniente del YAML.
     */
    private void validarReglas(List<QualityRule> reglas) {
        if (reglas == null || reglas.isEmpty()) {
            throw new QualityConfigException("No hay reglas de calidad configuradas");
        }
        Set<String> ids = new HashSet<>();
        double pesoTotal = 0;
        for (QualityRule rule : reglas) {
            if (StringUtils.isBlank(rule.getId())) {
                throw new QualityConfigException("Una regla no tiene ID definido");
            }
            if (!ids.add(rule.getId())) {
                throw new QualityConfigException("ID de regla duplicado: " + rule.getId());
            }
            pesoTotal += rule.getPeso();
        }
        if (pesoTotal <= 0) {
            throw new QualityConfigException("La suma de pesos debe ser mayor a cero");
        }
    }
}
