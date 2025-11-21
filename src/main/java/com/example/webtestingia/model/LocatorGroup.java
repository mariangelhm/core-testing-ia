package com.example.webtestingia.model;

import java.util.Map;

/**
 * Representa un grupo de locators cargados desde un YAML específico.
 */
public class LocatorGroup {

    private String nombre;
    private Map<String, String> locators;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Map<String, String> getLocators() {
        return locators;
    }

    public void setLocators(Map<String, String> locators) {
        this.locators = locators;
    }
}
