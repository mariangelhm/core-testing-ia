package com.example.webtestingia.controller;

import com.example.webtestingia.service.LocatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Expone los locators cargados por proyecto para consumo de clientes.
 */
@RestController
@RequestMapping("/api/proyectos/{proyecto}/locators")
public class LocatorController {

    private final LocatorService locatorService;

    public LocatorController(LocatorService locatorService) {
        this.locatorService = locatorService;
    }

    /**
     * Retorna todos los grupos disponibles para el proyecto.
     */
    @GetMapping
    public Map<String, Map<String, String>> listar(@PathVariable String proyecto) {
        return locatorService.obtenerGrupos(proyecto);
    }

    /**
     * Retorna un grupo específico.
     */
    @GetMapping("/{grupo}")
    public Map<String, String> obtenerGrupo(@PathVariable String proyecto, @PathVariable String grupo) {
        return locatorService.obtenerGrupo(proyecto, grupo);
    }
}
