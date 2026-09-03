package org.nepe;

import javafx.application.Application;
import org.nepe.bootstrap.JavaFxApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for NEPE (Nexus Exchange Prediction Engine).
 * <p>
 * Configures the Spring Boot application context and delegates lifecycle orchestration
 * to {@link JavaFxApplication} for desktop GUI presentation.
 */
@SpringBootApplication
public class NepeApplication {

    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}


// quando inserisco un match, ho un dropdown che mi mostra TUTTE le squadre presenti nel DB, dovremmo invece mostrare solo quelle della competizione.

// nel calcolo della forza di una squadra, vengono usati i soli dati della squadra stessa, ossia i calcoli avvengono su partite passate di quella stessa squadra. (xG)
