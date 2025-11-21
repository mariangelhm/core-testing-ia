package com.example.webtestingia.model.exception;

/**
 * Indica que el archivo de reglas de calidad no es válido o contiene
 * datos inconsistentes.
 */
public class QualityConfigException extends RuntimeException {

    public QualityConfigException(String message) {
        super(message);
    }

    public QualityConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
