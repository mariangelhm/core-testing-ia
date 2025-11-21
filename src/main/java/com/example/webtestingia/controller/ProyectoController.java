package com.example.webtestingia.controller;

import com.example.webtestingia.model.ProjectMetadata;
import com.example.webtestingia.model.QualityResult;
import com.example.webtestingia.model.exception.ProjectNotFoundException;
import com.example.webtestingia.service.CaseFileService;
import com.example.webtestingia.service.ProjectDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * APIs REST para exponer los metadatos de proyectos detectados
 * automáticamente en el sistema de archivos.
 */
@RestController
@RequestMapping("/api/proyectos")
public class ProyectoController {

    private static final Logger log = LoggerFactory.getLogger(ProyectoController.class);
    private final ProjectDiscoveryService discoveryService;
    private final CaseFileService caseFileService;

    public ProyectoController(ProjectDiscoveryService discoveryService, CaseFileService caseFileService) {
        this.discoveryService = discoveryService;
        this.caseFileService = caseFileService;
    }

    /**
     * Lista todos los proyectos existentes.
     */
    @GetMapping
    public List<ProjectMetadata> listar() {
        return discoveryService.listarProyectos();
    }

    /**
     * Obtiene detalles de un proyecto, incluyendo calidad promedio.
     */
    @GetMapping("/{proyecto}")
    public ResponseEntity<ProjectMetadata> obtener(@PathVariable String proyecto) {
        ProjectMetadata metadata = discoveryService.obtenerProyecto(proyecto);
        double promedio = caseFileService.listarCasos(proyecto).stream()
                .map(c -> c.getCalidad().getPuntaje())
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
        metadata.setCasos(List.of("Calidad promedio: " + promedio));
        return ResponseEntity.ok(metadata);
    }

    /**
     * Permite actualizar el archivo project.json.
     */
    @PutMapping("/{proyecto}")
    public ResponseEntity<ProjectMetadata> actualizar(@PathVariable String proyecto, @RequestBody ProjectMetadata metadata) {
        ProjectMetadata actualizado = discoveryService.actualizarProyecto(proyecto, metadata);
        return ResponseEntity.status(HttpStatus.OK).body(actualizado);
    }
}
