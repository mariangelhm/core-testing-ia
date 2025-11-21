package com.example.webtestingia.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado del análisis de calidad de un caso o proyecto.
 */
public class QualityResult {

    private double puntaje;
    private List<String> reglasCumplidas = new ArrayList<>();
    private List<String> reglasFalladas = new ArrayList<>();
    private List<String> sugerencias = new ArrayList<>();

    public double getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(double puntaje) {
        this.puntaje = puntaje;
    }

    public List<String> getReglasCumplidas() {
        return reglasCumplidas;
    }

    public void setReglasCumplidas(List<String> reglasCumplidas) {
        this.reglasCumplidas = reglasCumplidas;
    }

    public List<String> getReglasFalladas() {
        return reglasFalladas;
    }

    public void setReglasFalladas(List<String> reglasFalladas) {
        this.reglasFalladas = reglasFalladas;
    }

    public List<String> getSugerencias() {
        return sugerencias;
    }

    public void setSugerencias(List<String> sugerencias) {
        this.sugerencias = sugerencias;
    }
}
