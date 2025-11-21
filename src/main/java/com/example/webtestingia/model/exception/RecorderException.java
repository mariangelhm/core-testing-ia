package com.example.webtestingia.model.exception;

/**
 * Problemas específicos de la grabadora multiusuario.
 */
public class RecorderException extends RuntimeException {

    public RecorderException(String message) {
        super(message);
    }

    public RecorderException(String message, Throwable cause) {
        super(message, cause);
    }
}
