package com.example.webtestingia.drivers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

/**
 * Fabrica de WebDriver que lee la configuración desde config/navegador.yml
 * y permite crear drivers locales o remotos con esperas automáticas.
 */
@Component
public class WebDriverFactory {

    private static final Logger log = LoggerFactory.getLogger(WebDriverFactory.class);
    private static final String CONFIG_FILE = "config/navegador.yml";

    private final ThreadLocal<WebDriver> drivers = new ThreadLocal<>();
    private final BrowserConfig config;

    public WebDriverFactory() {
        this.config = cargarConfig();
    }

    /**
     * Obtiene un driver listo para usar con base en la configuración.
     * @return instancia de WebDriver.
     */
    public WebDriver getDriver() {
        if (drivers.get() == null) {
            drivers.set(crearDriver());
        }
        return drivers.get();
    }

    /**
     * Cierra y limpia el driver asociado al hilo.
     */
    public void cerrarDriver() {
        WebDriver driver = drivers.get();
        if (driver != null) {
            driver.quit();
            drivers.remove();
        }
    }

    /**
     * Crea un nuevo driver según la configuración de navegador.
     * @return driver listo para uso.
     */
    private WebDriver crearDriver() {
        log.info("Inicializando driver {} en modo {}", config.tipo, config.modo);
        try {
            if (config.autoDownloadDrivers) {
                configurarDescargas();
            }
            if ("grid".equalsIgnoreCase(config.modo)) {
                DesiredCapabilities capabilities = new DesiredCapabilities();
                capabilities.setBrowserName(config.tipo);
                capabilities.setPlatform(Platform.ANY);
                return new RemoteWebDriver(new URL(config.gridUrl), capabilities);
            }
            switch (config.tipo.toLowerCase()) {
                case "firefox":
                    return new FirefoxDriver();
                case "edge":
                    return new EdgeDriver();
                default:
                    return new ChromeDriver();
            }
        } catch (Exception e) {
            log.error("Error creando el driver", e);
            throw new RuntimeException("No se pudo inicializar el navegador", e);
        }
    }

    /**
     * Descarga automática de drivers cuando está habilitada.
     */
    private void configurarDescargas() {
        switch (config.tipo.toLowerCase()) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                break;
            case "edge":
                WebDriverManager.edgedriver().setup();
                break;
            default:
                WebDriverManager.chromedriver().setup();
                break;
        }
    }

    /**
     * Lee el YAML de navegador y arma la configuración interna.
     *
     * @return configuración de navegador.
     */
    private BrowserConfig cargarConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                throw new IllegalStateException("No se encontró config/navegador.yml");
            }
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(is);
            Map<String, Object> navegador = (Map<String, Object>) data.get("navegador");
            BrowserConfig cfg = new BrowserConfig();
            cfg.tipo = (String) navegador.getOrDefault("tipo", "chrome");
            cfg.modo = (String) navegador.getOrDefault("modo", "local");
            cfg.autoDownloadDrivers = Boolean.TRUE.equals(navegador.get("auto_download_drivers"));
            cfg.gridUrl = (String) navegador.getOrDefault("grid_url", "http://localhost:4444/wd/hub");
            return cfg;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer config/navegador.yml", e);
        }
    }

    /**
     * Configuración interna del navegador.
     */
    public static class BrowserConfig {
        public String tipo;
        public String modo;
        public boolean autoDownloadDrivers;
        public String gridUrl;
    }
}
