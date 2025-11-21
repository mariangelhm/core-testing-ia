package com.example.webtestingia.model;

/**
 * Detalle completo de un caso de prueba incluyendo el contenido
 * y el análisis de calidad asociado.
 */
public class CaseDetail {

    private String contenido;
    private QualityResult calidad;

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public QualityResult getCalidad() {
        return calidad;
    }

    public void setCalidad(QualityResult calidad) {
        this.calidad = calidad;
    }
}
