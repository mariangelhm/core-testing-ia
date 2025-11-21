package com.example.webtestingia.service;

import com.example.webtestingia.model.ProjectMetadata;
import com.example.webtestingia.model.exception.FileProcessingException;
import com.example.webtestingia.model.exception.ProjectNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Descubre proyectos a partir de carpetas bajo src/test/resources/features
 * sin utilizar base de datos, manteniendo y actualizando project.json.
 */
@Service
public class ProjectDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ProjectDiscoveryService.class);
    private static final String FEATURES_PATH = "src/test/resources/features";
    private static final String PROJECT_FILE = "project.json";

    private final ObjectMapper objectMapper;

    public ProjectDiscoveryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Lista todos los proyectos detectados en el filesystem.
     *
     * @return metadatos de cada proyecto.
     */
    public List<ProjectMetadata> listarProyectos() {
        try {
            ensureBasePath();
            return Files.list(Path.of(FEATURES_PATH))
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(this::cargarProyectoOGenerar)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error al recorrer proyectos", e);
            throw new FileProcessingException("No se pudieron listar los proyectos", e);
        }
    }

    /**
     * Obtiene un proyecto por nombre.
     *
     * @param nombre nombre de la carpeta.
     * @return metadatos leídos desde project.json.
     */
    public ProjectMetadata obtenerProyecto(String nombre) {
        Path path = Path.of(FEATURES_PATH, nombre);
        if (!Files.isDirectory(path)) {
            throw new ProjectNotFoundException("No existe el proyecto " + nombre);
        }
        return cargarProyectoOGenerar(nombre);
    }

    /**
     * Actualiza el archivo project.json de un proyecto existente.
     *
     * @param nombre   carpeta del proyecto.
     * @param metadata datos a persistir.
     * @return metadatos ya guardados.
     */
    public ProjectMetadata actualizarProyecto(String nombre, ProjectMetadata metadata) {
        Path path = Path.of(FEATURES_PATH, nombre);
        if (!Files.isDirectory(path)) {
            throw new ProjectNotFoundException("No existe el proyecto " + nombre);
        }
        Path projectFile = path.resolve(PROJECT_FILE);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(projectFile.toFile(), metadata);
            return metadata;
        } catch (IOException e) {
            log.error("No se pudo escribir project.json de {}", nombre, e);
            throw new FileProcessingException("Error escribiendo project.json", e);
        }
    }

    /**
     * Lee el archivo project.json si existe, o lo crea con valores por defecto
     * cuando falta en disco.
     *
     * @param nombre carpeta del proyecto.
     * @return metadatos consistentes.
     */
    private ProjectMetadata cargarProyectoOGenerar(String nombre) {
        Path projectFile = Path.of(FEATURES_PATH, nombre, PROJECT_FILE);
        if (!Files.exists(projectFile)) {
            log.warn("project.json no encontrado para {}, generando valores por defecto", nombre);
            ProjectMetadata metadata = ProjectMetadata.defaultFor(nombre);
            try {
                Files.createDirectories(projectFile.getParent());
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(projectFile.toFile(), metadata);
            } catch (IOException e) {
                throw new FileProcessingException("No se pudo crear project.json para " + nombre, e);
            }
            return metadata;
        }
        try {
            return objectMapper.readValue(projectFile.toFile(), ProjectMetadata.class);
        } catch (IOException e) {
            log.error("project.json corrupto para {}", nombre, e);
            throw new FileProcessingException("Error leyendo project.json de " + nombre, e);
        }
    }

    /**
     * Garantiza que la carpeta base de features exista en el sistema de archivos.
     *
     * @throws IOException cuando no es posible crear la carpeta.
     */
    private void ensureBasePath() throws IOException {
        if (!Files.exists(Path.of(FEATURES_PATH))) {
            Files.createDirectories(Path.of(FEATURES_PATH));
        }
    }
}
