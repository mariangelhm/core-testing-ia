package com.example.webtestingia.service;

import com.example.webtestingia.model.exception.InvalidLocatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carga y cachea los archivos YAML de locators por proyecto.
 */
@Service
public class LocatorService {

    private static final Logger log = LoggerFactory.getLogger(LocatorService.class);
    private static final String LOCATOR_PATH = "src/main/resources/locators";

    private final Map<String, Map<String, Map<String, String>>> cache = new ConcurrentHashMap<>();

    /**
     * Obtiene todos los grupos de un proyecto.
     */
    public Map<String, Map<String, String>> obtenerGrupos(String proyecto) {
        return cache.computeIfAbsent(proyecto, this::cargarProyecto);
    }

    /**
     * Obtiene un grupo específico de locators.
     */
    public Map<String, String> obtenerGrupo(String proyecto, String grupo) {
        Map<String, Map<String, String>> grupos = obtenerGrupos(proyecto);
        Map<String, String> resultado = grupos.get(grupo);
        if (resultado == null) {
            throw new InvalidLocatorException("Grupo de locators no encontrado: " + grupo);
        }
        return resultado;
    }

    /**
     * Resuelve un locator específico por nombre.
     */
    public String resolveLocator(String proyecto, String grupo, String nombreLocator) {
        Map<String, String> grupoMap = obtenerGrupo(proyecto, grupo);
        String selector = grupoMap.get(nombreLocator);
        if (selector == null) {
            throw new InvalidLocatorException("Locator no encontrado: " + nombreLocator);
        }
        return selector;
    }

    /**
     * Lee todos los YAML de locators de un proyecto y arma el mapa de grupos.
     *
     * @param proyecto nombre del proyecto.
     * @return mapa de grupos a locators.
     */
    private Map<String, Map<String, String>> cargarProyecto(String proyecto) {
        Map<String, Map<String, String>> grupos = new HashMap<>();
        Path base = Path.of(LOCATOR_PATH, proyecto);
        if (!Files.exists(base)) {
            log.warn("Proyecto sin carpeta de locators: {}", proyecto);
            return Collections.emptyMap();
        }
        try {
            Yaml yaml = new Yaml();
            Files.list(base)
                    .filter(p -> p.toString().endsWith(".yml"))
                    .forEach(path -> {
                        try (InputStream is = Files.newInputStream(path)) {
                            Map<String, Map<String, String>> data = yaml.load(is);
                            if (data != null) {
                                grupos.putAll(data);
                            }
                        } catch (IOException e) {
                            throw new InvalidLocatorException("YAML de locators corrupto: " + path.getFileName());
                        }
                    });
            return grupos;
        } catch (IOException e) {
            throw new InvalidLocatorException("No se pudieron cargar los locators de " + proyecto);
        }
    }
}
