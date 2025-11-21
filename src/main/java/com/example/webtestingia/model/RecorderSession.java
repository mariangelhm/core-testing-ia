package com.example.webtestingia.model;

import org.openqa.selenium.WebDriver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Contiene la información en memoria para una sesión de grabación
 * multiusuario.
 */
public class RecorderSession {

    private String id;
    private WebDriver driver;
    private List<String> steps = new ArrayList<>();
    private Instant createdAt = Instant.now();
    private String usuario;
    private String proyecto;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public WebDriver getDriver() {
        return driver;
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getProyecto() {
        return proyecto;
    }

    public void setProyecto(String proyecto) {
        this.proyecto = proyecto;
    }
}
