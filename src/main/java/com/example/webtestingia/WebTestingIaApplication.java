package com.example.webtestingia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación principal para el backend de Web Testing IA.
 * Expone servicios REST, inicializa la configuración de Spring Boot
 * y habilita el escaneo de componentes en el paquete base.
 */
@SpringBootApplication
public class WebTestingIaApplication {

    /**
     * Punto de entrada estándar de Spring Boot.
     * @param args argumentos de línea de comando.
     */
    public static void main(String[] args) {
        SpringApplication.run(WebTestingIaApplication.class, args);
    }
}
