package com.example.webtestingia.model;

import java.time.Instant;

/**
 * Respuesta estandarizada para errores específicos expuestos por las APIs.
 */
public class ApiError {

    private Instant timestamp = Instant.now();
    private String mensaje;
    private String detalle;

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
