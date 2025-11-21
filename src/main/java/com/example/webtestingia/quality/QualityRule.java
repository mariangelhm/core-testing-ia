package com.example.webtestingia.quality;

/**
 * Regla de calidad configurable que puede activarse o desactivarse
 * según el archivo quality-rules.yml.
 */
public class QualityRule {

    private String id;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private double peso;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }
}
