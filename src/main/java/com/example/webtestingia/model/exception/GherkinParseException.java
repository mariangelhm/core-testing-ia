package com.example.webtestingia.model.exception;

/**
 * Error específico al interpretar contenido Gherkin de un archivo .feature.
 */
public class GherkinParseException extends RuntimeException {

    public GherkinParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public GherkinParseException(String message) {
        super(message);
    }
}
