package com.example.webtestingia.model.exception;

/**
 * Error específico relacionado con operaciones de lectura o escritura
 * en el sistema de archivos.
 */
public class FileProcessingException extends RuntimeException {

    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
