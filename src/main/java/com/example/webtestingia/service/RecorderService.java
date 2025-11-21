package com.example.webtestingia.service;

import com.example.webtestingia.model.QualityResult;
import com.example.webtestingia.model.RecorderEvent;
import com.example.webtestingia.model.RecorderSession;
import com.example.webtestingia.model.exception.RecorderException;
import com.example.webtestingia.quality.QualityAnalyzer;
import com.example.webtestingia.recorder.RecorderSessionManager;
import com.example.webtestingia.recorder.StepMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquesta la creación y cierre de sesiones de grabación, delegando la
 * conversión de eventos en pasos Gherkin al StepMapper.
 */
@Service
public class RecorderService {

    private static final Logger log = LoggerFactory.getLogger(RecorderService.class);

    private final StepMapper stepMapper;
    private final RecorderSessionManager sessionManager;
    private final QualityAnalyzer qualityAnalyzer;

    public RecorderService(StepMapper stepMapper, RecorderSessionManager sessionManager, QualityAnalyzer qualityAnalyzer) {
        this.stepMapper = stepMapper;
        this.sessionManager = sessionManager;
        this.qualityAnalyzer = qualityAnalyzer;
    }

    /**
     * Crea una sesión nueva utilizando el driver suministrado.
     */
    public String iniciarGrabacion(org.openqa.selenium.WebDriver driver) {
        return sessionManager.createSession(driver);
    }

    /**
     * Procesa un evento entrante y lo agrega como paso a la sesión indicada.
     */
    public void procesarEvento(RecorderEvent event, String grupoActual) {
        RecorderSession session = sessionManager.getSession(event.getSessionId());
        String step = stepMapper.map(event.getAction(), event.getSelector(), event.getText(), grupoActual);
        log.info("Evento {} traducido a step {}", event.getAction(), step);
        session.getSteps().add(step);
    }

    /**
     * Devuelve los pasos acumulados de una sesión.
     */
    public List<String> obtenerSteps(String sessionId) {
        return sessionManager.getSession(sessionId).getSteps();
    }

    /**
     * Cierra la sesión y evalúa la calidad del flujo generado.
     */
    public SessionSummary detenerSesion(String sessionId) {
        RecorderSession session = sessionManager.closeSession(sessionId);
        if (session == null) {
            throw new RecorderException("No se pudo cerrar la sesión: " + sessionId);
        }
        String escenario = construirEscenario(session.getSteps());
        QualityResult calidad = qualityAnalyzer.analizarCaso(escenario);
        SessionSummary summary = new SessionSummary();
        summary.steps = session.getSteps();
        summary.qualityResult = calidad;
        return summary;
    }

    /**
     * Construye un escenario Gherkin a partir de los pasos grabados.
     *
     * @param steps pasos capturados.
     * @return texto completo en formato Feature.
     */
    private String construirEscenario(List<String> steps) {
        StringBuilder builder = new StringBuilder();
        builder.append("Feature: Escenario grabado\n\n");
        builder.append("  Scenario: Flujo generado por grabadora\n");
        for (String step : steps) {
            builder.append("    ").append(step).append("\n");
        }
        return builder.toString();
    }

    /**
     * Estructura de respuesta al finalizar la grabación.
     */
    public static class SessionSummary {
        public List<String> steps;
        public QualityResult qualityResult;
    }
}
