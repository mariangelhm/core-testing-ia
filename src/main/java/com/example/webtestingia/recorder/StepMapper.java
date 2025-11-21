package com.example.webtestingia.recorder;

import com.example.webtestingia.service.LocatorService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Convierte eventos crudos de navegador en pasos Gherkin reutilizables
 * respetando la convención de locators por grupo o selectores directos.
 */
@Component
public class StepMapper {

    private final LocatorService locatorService;

    public StepMapper(LocatorService locatorService) {
        this.locatorService = locatorService;
    }

    /**
     * Transforma un evento en una línea Gherkin.
     * @param action tipo de acción capturada.
     * @param selector objetivo (locator o xpath/css).
     * @param valor texto asociado a la acción.
     * @param grupo grupo de locators en contexto (opcional).
     * @return línea en lenguaje Gherkin lista para usarse en un .feature.
     */
    public String map(String action, String selector, String valor, String grupo) {
        String objetivo = normalizarSelector(selector, grupo);
        switch (action) {
            case "click":
                return String.format("When hago clic en \"%s\"", objetivo);
            case "input":
            case "change":
                return String.format("When escribo \"%s\" en \"%s\"", valor, objetivo);
            case "navigate":
                return String.format("Given navego a \"%s\"", valor);
            default:
                return String.format("# Acción no mapeada: %s %s", action, selector);
        }
    }

    /**
     * Determina si el objetivo es un selector directo o un nombre de locator.
     *
     * @param selector selector recibido desde el frontend.
     * @param grupo    grupo de locators en contexto.
     * @return texto que debe usarse en el step generado.
     */
    private String normalizarSelector(String selector, String grupo) {
        if (selector == null) {
            return "";
        }
        String trimmed = selector.trim();
        if (trimmed.startsWith("//") || trimmed.startsWith("css=") || trimmed.startsWith("xpath=")) {
            return trimmed;
        }
        if (StringUtils.isNotBlank(grupo)) {
            try {
                locatorService.resolveLocator("default", grupo, trimmed);
            } catch (Exception ignored) {
                // Si el locator no existe, se devuelve el texto plano.
            }
        }
        return trimmed;
    }
}
