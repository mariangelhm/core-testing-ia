package com.example.webtestingia.hooks;

import com.example.webtestingia.drivers.WebDriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hooks de Cucumber para preparar y cerrar el WebDriver de forma segura.
 */
@Component
public class CucumberHooks {

    private static final Logger log = LoggerFactory.getLogger(CucumberHooks.class);
    private final WebDriverFactory webDriverFactory;

    public CucumberHooks(WebDriverFactory webDriverFactory) {
        this.webDriverFactory = webDriverFactory;
    }

    /**
     * Hook previo que inicializa el driver cuando se requiere.
     */
    @Before
    public void beforeScenario() {
        log.info("Preparando driver para el escenario");
        webDriverFactory.getDriver();
    }

    /**
     * Hook posterior que cierra el driver y libera recursos.
     */
    @After
    public void afterScenario() {
        log.info("Cerrando driver tras escenario");
        webDriverFactory.cerrarDriver();
    }
}
