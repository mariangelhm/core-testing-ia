package com.example.webtestingia.model.exception;

/**
 * Se utiliza cuando un locator no puede resolverse o está mal formado
 * en los archivos YAML.
 */
public class InvalidLocatorException extends RuntimeException {

    public InvalidLocatorException(String message) {
        super(message);
    }
}
