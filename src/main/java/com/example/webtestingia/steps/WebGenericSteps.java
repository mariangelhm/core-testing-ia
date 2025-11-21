package com.example.webtestingia.steps;

import com.example.webtestingia.drivers.WebDriverFactory;
import com.example.webtestingia.service.LocatorService;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Steps genéricos que permiten usar locators YAML o selectores directos.
 */
@Component
public class WebGenericSteps {

    private static final Logger log = LoggerFactory.getLogger(WebGenericSteps.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final WebDriverFactory webDriverFactory;
    private final LocatorService locatorService;
    private String grupoActual;

    public WebGenericSteps(WebDriverFactory webDriverFactory, LocatorService locatorService) {
        this.webDriverFactory = webDriverFactory;
        this.locatorService = locatorService;
    }

    /**
     * Define el grupo de locators a utilizar en los pasos siguientes.
     */
    @Dado("establezco el grupo {string}")
    public void establezcoElGrupo(String grupo) {
        this.grupoActual = grupo;
        log.info("Grupo de locators activo: {}", grupo);
    }

    /**
     * Ejecuta un clic utilizando el locator resuelto o selector directo.
     */
    @Cuando("hago clic en {string}")
    public void hagoClicEn(String objetivo) {
        WebDriver driver = webDriverFactory.getDriver();
        By by = resolverSelector(objetivo);
        log.info("Haciendo clic en {} -> {}", objetivo, by);
        WebElement element = new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(by));
        element.click();
    }

    /**
     * Escribe texto en el elemento destino con esperas dinámicas.
     */
    @Cuando("escribo {string} en {string}")
    public void escriboEn(String texto, String objetivo) {
        WebDriver driver = webDriverFactory.getDriver();
        By by = resolverSelector(objetivo);
        log.info("Escribiendo en {} el valor {}", by, texto);
        WebElement element = new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(by));
        element.clear();
        element.sendKeys(texto);
    }

    /**
     * Valida la presencia de un texto en la página.
     */
    @Entonces("debería ver el texto {string}")
    public void deberiaVerElTexto(String texto) {
        WebDriver driver = webDriverFactory.getDriver();
        log.info("Validando texto visible: {}", texto);
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), texto));
    }

    /**
     * Determina el tipo de selector a usar, resolviendo YAML o usando selectores directos.
     *
     * @param objetivo locator o selector recibido en el step.
     * @return objeto By utilizable por Selenium.
     */
    private By resolverSelector(String objetivo) {
        String selector = objetivo.trim();
        if (selector.startsWith("//")) {
            return By.xpath(selector);
        }
        if (selector.startsWith("css=")) {
            return By.cssSelector(selector.substring(4));
        }
        if (selector.startsWith("xpath=")) {
            return By.xpath(selector.substring(6));
        }
        if (grupoActual != null) {
            try {
                String resolved = locatorService.resolveLocator("default", grupoActual, selector);
                return resolved.startsWith("//") ? By.xpath(resolved) : By.cssSelector(resolved.replace("css=", ""));
            } catch (Exception e) {
                log.warn("No se pudo resolver locator {}, usando texto literal", selector);
            }
        }
        return selector.startsWith("//") ? By.xpath(selector) : By.cssSelector(selector);
    }
}
