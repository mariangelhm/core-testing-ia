package com.example.webtestingia.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de un caso Gherkin encontrado en el sistema de archivos.
 */
public class CaseSummary {

    private String ruta;
    private String escenario;
    private List<String> tags = new ArrayList<>();
    private QualityResult calidad;

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public String getEscenario() {
        return escenario;
    }

    public void setEscenario(String escenario) {
        this.escenario = escenario;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public QualityResult getCalidad() {
        return calidad;
    }

    public void setCalidad(QualityResult calidad) {
        this.calidad = calidad;
    }
}
