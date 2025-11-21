package com.example.webtestingia.steps;

import com.company.qa.core.db.DBHelper;
import com.company.qa.core.logging.LoggerUtil;
import com.company.qa.core.rest.RestServiceClient;
import io.cucumber.java.es.Dado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Steps que delegan la ejecución de precondiciones a la librería qa-core.
 */
@Component
public class PreconditionSteps {

    private static final Logger log = LoggerFactory.getLogger(PreconditionSteps.class);

    /**
     * Ejecuta una query SQL predefinida utilizando DBHelper.
     */
    @Dado("ejecuto la query previa {string}")
    public void ejecutoQueryPrevia(String nombre) {
        try {
            log.info("Ejecutando precondición de query: {}", nombre);
            DBHelper dbHelper = new DBHelper();
            dbHelper.executePredefinedQuery(nombre);
        } catch (Exception e) {
            LoggerUtil.error("Error en precondición de base de datos", e);
            throw new RuntimeException("Fallo ejecutando la query previa: " + nombre, e);
        }
    }

    /**
     * Ejecuta un servicio REST predefinido usando RestServiceClient.
     */
    @Dado("ejecuto el servicio previo {string}")
    public void ejecutoServicioPrevio(String nombre) {
        try {
            log.info("Ejecutando precondición de servicio: {}", nombre);
            RestServiceClient client = new RestServiceClient();
            client.executePredefinedService(nombre);
        } catch (Exception e) {
            LoggerUtil.error("Error en precondición de servicio", e);
            throw new RuntimeException("Fallo ejecutando el servicio previo: " + nombre, e);
        }
    }
}
