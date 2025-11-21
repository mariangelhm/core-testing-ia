package com.example.webtestingia.controller;

import com.example.webtestingia.drivers.WebDriverFactory;
import com.example.webtestingia.model.RecorderEvent;
import com.example.webtestingia.model.exception.RecorderException;
import com.example.webtestingia.service.RecorderService;
import org.openqa.selenium.WebDriver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la grabadora multiusuario.
 */
@RestController
@RequestMapping("/api/recorder")
public class RecorderController {

    private final RecorderService recorderService;
    private final WebDriverFactory webDriverFactory;

    public RecorderController(RecorderService recorderService, WebDriverFactory webDriverFactory) {
        this.recorderService = recorderService;
        this.webDriverFactory = webDriverFactory;
    }

    /**
     * Inicia una nueva sesión de grabación.
     */
    @PostMapping("/start")
    public Map<String, String> start() {
        WebDriver driver = webDriverFactory.getDriver();
        String sessionId = recorderService.iniciarGrabacion(driver);
        return Map.of("sessionId", sessionId);
    }

    /**
     * Recibe eventos provenientes del frontend grabador.
     */
    @PostMapping("/event")
    public ResponseEntity<Void> registrarEvento(@RequestBody RecorderEvent event, @RequestParam(required = false) String grupo) {
        recorderService.procesarEvento(event, grupo);
        return ResponseEntity.accepted().build();
    }

    /**
     * Lista los pasos generados hasta ahora.
     */
    @GetMapping("/steps")
    public java.util.List<String> steps(@RequestParam String sessionId) {
        return recorderService.obtenerSteps(sessionId);
    }

    /**
     * Detiene la sesión y devuelve los pasos más sugerencias de calidad.
     */
    @PostMapping("/stop")
    public ResponseEntity<RecorderService.SessionSummary> stop(@RequestParam String sessionId) {
        RecorderService.SessionSummary summary = recorderService.detenerSesion(sessionId);
        return ResponseEntity.status(HttpStatus.OK).body(summary);
    }
}
