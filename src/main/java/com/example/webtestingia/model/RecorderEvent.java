package com.example.webtestingia.model;

/**
 * Evento recibido desde el navegador grabado para transformarlo
 * en pasos Gherkin reutilizables.
 */
public class RecorderEvent {

    private String sessionId;
    private String action;
    private String selector;
    private String text;
    private String value;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
