package com.example.webtestingia.quality;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa la estructura del archivo quality-rules.yml
 * permitiendo que Spring lea la lista de reglas configuradas.
 */
public class QualityRulesConfig {

    private List<QualityRule> reglas = new ArrayList<>();

    public List<QualityRule> getReglas() {
        return reglas;
    }

    public void setReglas(List<QualityRule> reglas) {
        this.reglas = reglas;
    }
}
