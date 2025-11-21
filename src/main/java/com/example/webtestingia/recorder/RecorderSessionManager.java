package com.example.webtestingia.recorder;

import com.example.webtestingia.model.RecorderSession;
import com.example.webtestingia.model.exception.RecorderException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Administra sesiones de grabación aisladas por usuario utilizando un mapa concurrente.
 */
@Component
public class RecorderSessionManager {

    private static final Logger log = LoggerFactory.getLogger(RecorderSessionManager.class);
    private final Map<String, RecorderSession> sessions = new ConcurrentHashMap<>();

    /**
     * Crea una nueva sesión usando el driver entregado por el consumidor.
     * @param driver navegador dedicado.
     * @return identificador único de sesión.
     */
    public String createSession(WebDriver driver) {
        String id = UUID.randomUUID().toString();
        RecorderSession session = new RecorderSession();
        session.setId(id);
        session.setDriver(driver);
        sessions.put(id, session);
        return id;
    }

    /**
     * Obtiene una sesión existente o arroja excepción.
     */
    public RecorderSession getSession(String id) {
        RecorderSession session = sessions.get(id);
        if (session == null) {
            throw new RecorderException("Sesión no encontrada: " + id);
        }
        return session;
    }

    /**
     * Agrega un step generado a la sesión indicada.
     */
    public void addStep(String sessionId, String step) {
        getSession(sessionId).getSteps().add(step);
    }

    /**
     * Cierra la sesión, liberando el driver y limpiando el mapa.
     */
    public RecorderSession closeSession(String id) {
        RecorderSession session = sessions.remove(id);
        if (session != null && session.getDriver() != null) {
            session.getDriver().quit();
        }
        return session;
    }

    /**
     * Limpia sesiones con más del TTL indicado.
     */
    public void limpiarSesionesAntiguas(Duration ttl) {
        Instant ahora = Instant.now();
        sessions.values().removeIf(session -> Duration.between(session.getCreatedAt(), ahora).compareTo(ttl) > 0);
    }
}
