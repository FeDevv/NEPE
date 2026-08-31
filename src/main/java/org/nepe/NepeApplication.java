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


/*
import sembrerebbe funzionare, ma come importo una stagione precedente?
ho importato sia la stagione 2026/2027 (attuale) che 2025/2026, ma mi ha raggruppato tutte
le partite in una sola stagione, la 2025/2026. cosa ho sbagliato?
il "nome" della stagione deve essere nel nome del file CSV?

inoltre mi da, nella schermata pre-match, l'alert "seasonal decay gamma must be in range (0.0 , 1.0] (received -0.12)"
proprio come se stesse ricevendo il rho di dixon-coles.

il bottone "pre match" risulta cliccabile anche se la parita è finita (non è un errore grave, anzi, però a partita finita
non si può fare trading, dunque non ha senso avere accesso in questo modo).
* */

// check SonarCloud to complete project.