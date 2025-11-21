package com.example.webtestingia.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa el archivo project.json con los metadatos principales
 * de un proyecto detectado dentro de la carpeta de features.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectMetadata {

    private String id;
    private String nombre;
    private String codigoJira;
    private String tipo;
    private String autor;
    private String editor;
    private List<String> casos = new ArrayList<>();

    /**
     * Construye un objeto vacío requerido por Jackson.
     */
    public ProjectMetadata() {
    }

    /**
     * Genera una instancia con valores por defecto cuando el archivo
     * no existe en disco.
     *
     * @param projectName nombre de la carpeta de proyecto.
     * @return metadatos completos con identificador generado.
     */
    public static ProjectMetadata defaultFor(String projectName) {
        ProjectMetadata metadata = new ProjectMetadata();
        metadata.setId(UUID.randomUUID().toString());
        metadata.setNombre(projectName);
        metadata.setCodigoJira(projectName.toUpperCase());
        metadata.setTipo("WEB");
        metadata.setAutor("desconocido");
        metadata.setEditor("desconocido");
        return metadata;
    }

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

    public String getCodigoJira() {
        return codigoJira;
    }

    public void setCodigoJira(String codigoJira) {
        this.codigoJira = codigoJira;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getEditor() {
        return editor;
    }

    public void setEditor(String editor) {
        this.editor = editor;
    }

    public List<String> getCasos() {
        return casos;
    }

    public void setCasos(List<String> casos) {
        this.casos = casos;
    }
}
