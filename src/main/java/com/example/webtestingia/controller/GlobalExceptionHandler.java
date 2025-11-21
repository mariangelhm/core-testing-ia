package com.example.webtestingia.controller;

import com.example.webtestingia.model.ApiError;
import com.example.webtestingia.model.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo centralizado de errores para proporcionar mensajes claros y
 * códigos HTTP específicos en cada escenario.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja errores de proyectos inexistentes.
     */
    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiError> handleProject(ProjectNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex);
    }

    /**
     * Maneja problemas con locators.
     */
    @ExceptionHandler(InvalidLocatorException.class)
    public ResponseEntity<ApiError> handleLocator(InvalidLocatorException ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * Maneja errores de archivo.
     */
    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ApiError> handleFile(FileProcessingException ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    /**
     * Maneja errores de configuración de calidad.
     */
    @ExceptionHandler(QualityConfigException.class)
    public ResponseEntity<ApiError> handleQuality(QualityConfigException ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * Maneja errores al interpretar Gherkin.
     */
    @ExceptionHandler(GherkinParseException.class)
    public ResponseEntity<ApiError> handleGherkin(GherkinParseException ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * Maneja errores de la grabadora.
     */
    @ExceptionHandler(RecorderException.class)
    public ResponseEntity<ApiError> handleRecorder(RecorderException ex) {
        return build(HttpStatus.CONFLICT, ex);
    }

    /**
     * Handler genérico para casos no previstos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    /**
     * Construye la respuesta API estándar para un error controlado.
     *
     * @param status código HTTP a devolver.
     * @param ex     excepción original.
     * @return entidad con cuerpo ApiError.
     */
    private ResponseEntity<ApiError> build(HttpStatus status, Exception ex) {
        log.error("Error controlado: {}", ex.getMessage(), ex);
        ApiError apiError = new ApiError();
        apiError.setMensaje(ex.getMessage());
        apiError.setDetalle(ex.getClass().getSimpleName());
        return ResponseEntity.status(status).body(apiError);
    }
}
