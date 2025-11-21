package com.example.webtestingia.model.exception;

/**
 * Excepción lanzada cuando el proyecto solicitado no existe
 * dentro de la carpeta de features configurada.
 */
public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String message) {
        super(message);
    }
}
