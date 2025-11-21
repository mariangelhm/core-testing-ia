package com.example.webtestingia.controller;

import com.example.webtestingia.model.CaseDetail;
import com.example.webtestingia.model.CaseSummary;
import com.example.webtestingia.service.CaseFileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints para gestionar archivos .feature de los proyectos web.
 */
@RestController
@RequestMapping("/api/proyectos/{proyecto}/casos-web")
public class CasoWebController {

    private final CaseFileService caseFileService;

    public CasoWebController(CaseFileService caseFileService) {
        this.caseFileService = caseFileService;
    }

    /**
     * Lista todos los casos del proyecto.
     */
    @GetMapping
    public List<CaseSummary> listarCasos(@PathVariable String proyecto) {
        return caseFileService.listarCasos(proyecto);
    }

    /**
     * Lee un caso concreto, devolviendo su contenido y calidad.
     */
    @GetMapping("/{ruta}")
    public CaseDetail obtenerCaso(@PathVariable String proyecto, @PathVariable String ruta) {
        return caseFileService.leerCaso(proyecto, ruta);
    }

    /**
     * Crea un caso nuevo.
     */
    @PostMapping
    public ResponseEntity<Void> crearCaso(@PathVariable String proyecto, @RequestParam String ruta, @RequestBody String contenido) {
        caseFileService.crearCaso(proyecto, ruta, contenido);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Actualiza un caso existente.
     */
    @PutMapping("/{ruta}")
    public ResponseEntity<Void> actualizarCaso(@PathVariable String proyecto, @PathVariable String ruta, @RequestBody String contenido) {
        caseFileService.actualizarCaso(proyecto, ruta, contenido);
        return ResponseEntity.ok().build();
    }

    /**
     * Elimina un archivo o escenario.
     */
    @DeleteMapping("/{ruta}")
    public ResponseEntity<Void> eliminarCaso(@PathVariable String proyecto, @PathVariable String ruta) {
        caseFileService.eliminarCaso(proyecto, ruta);
        return ResponseEntity.noContent().build();
    }
}
